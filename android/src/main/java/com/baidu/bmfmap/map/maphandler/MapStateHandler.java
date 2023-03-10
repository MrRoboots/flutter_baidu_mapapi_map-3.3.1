package com.baidu.bmfmap.map.maphandler;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.baidu.bmfmap.BMFMapController;
import com.baidu.bmfmap.map.FlutterMapViewWrapper;
import com.baidu.bmfmap.utils.Constants;
import com.baidu.bmfmap.utils.converter.FlutterDataConveter;
import com.baidu.bmfmap.utils.converter.TypeConverter;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.LogoPosition;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.WinRound;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.TextUtils;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class MapStateHandler extends BMapHandler {
    private static final String TAG = MapStateHandler.class.getSimpleName();
    private FlutterMapViewWrapper mFlutterMapViewWrapper;

    public MapStateHandler(BMFMapController bmfMapController) {
        super(bmfMapController);
        mFlutterMapViewWrapper = bmfMapController.getFlutterMapViewWrapper();
    }

    @Override
    public void handlerMethodCallResult(Context context, MethodCall call,
                                        MethodChannel.Result result) {
        if (null == call) {
            return;
        }
        String methodId = call.method;
        if (TextUtils.isEmpty(methodId)) {
            return;
        }

        switch (methodId) {
            case Constants.MethodProtocol.MapStateProtocol.sMapUpdateMethod:
                setMapUpdate(call, result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.sMapTakeSnapshotMethod:
                mapSnapshot(result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.sMapTakeSnapshotWithRectMethod:
                snapShotRect(call, result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.sMapSetCompassImageMethod:
                setCompassImage(call, result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.sMapSetCustomTrafficColorMethod:
                setCustomTrafficColor(call, result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.sMapSetVisibleMapBoundsMethod:
                setNewCoordinateBounds(call, result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.sMapSetVisibleMapBoundsWithPaddingMethod:
                setVisibleMapBoundsWithPaddingMethod(call, result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.MAP_COPYRINGHT_INFO:
                getMapCopyrightInfo(result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.MAP_MAPPING_QUALIFICATION:
                getMapMappingQualification(result);
                break;
            case Constants.MethodProtocol.MapStateProtocol.MAP_APPROVAL_NUMBER:
                getMapApprovalNumber(result);
                break;
            default:
                break;
        }
    }

    private void getMapApprovalNumber(MethodChannel.Result result) {
        if (null == mBaiduMap || null == result) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        map.put("approvalNumber", mBaiduMap.getMapApprovalNumber());
        result.success(map);
    }

    private void getMapMappingQualification(MethodChannel.Result result) {
        if (null == mBaiduMap || null == result) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        map.put("mappingQualification", mBaiduMap.getMapMappingQualificationInfo());
        result.success(map);
    }

    private void getMapCopyrightInfo(MethodChannel.Result result) {
        if (null == mBaiduMap || null == result) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        map.put("copyrightInformation", mBaiduMap.getMapCopyrightInfo());
        result.success(map);
    }


    /**
     * ?????????????????????
     */
    private void setCustomTrafficColor(MethodCall call, MethodChannel.Result result) {
        Map<String, Object> argument = call.arguments();
        if (null == argument || null == mBaiduMap) {
            result.success(false);
            return;
        }
        if (!argument.containsKey("smooth") || !argument.containsKey("slow")
                || !argument.containsKey("congestion") || !argument
                .containsKey("severeCongestion")) {
            result.success(false);
            return;
        }

        String smooth = (String) argument.get("smooth");
        String slow = (String) argument.get("slow");
        String congestion = (String) argument.get("congestion");
        String severeCongestion = (String) argument.get("severeCongestion");
        if (smooth == null || slow == null || congestion == null || severeCongestion == null) {
            result.success(false);
            return;
        }
        String color = "#";
        String severeCongestionColor = color.concat(severeCongestion);
        String congestionColor = color.concat(congestion);
        String slowColor = color.concat(slow);
        String smoothColor = color.concat(smooth);

        mBaiduMap.setCustomTrafficColor(severeCongestionColor, congestionColor, slowColor,
                smoothColor);
        result.success(true);
    }

    /**
     * ??????????????????
     */
    private void setCompassImage(MethodCall call, MethodChannel.Result result) {
        Map<String, Object> argument = call.arguments();
        if (null == argument || null == mBaiduMap) {
            result.success(false);
            return;
        }

        if (!argument.containsKey("imagePath")) {
            result.success(false);
            return;
        }

        String imagePath = (String) argument.get("imagePath");
        if (imagePath == null) {
            result.success(false);
            return;
        }
        BitmapDescriptor bitmapDescriptor =
                BitmapDescriptorFactory.fromAsset("flutter_assets/" + imagePath);
        Bitmap bitmap = bitmapDescriptor.getBitmap();
        mBaiduMap.setCompassIcon(bitmap);
        result.success(true);
    }

    /**
     * ??????????????????
     */
    private void snapShotRect(MethodCall call, final MethodChannel.Result result) {
        Map<String, Object> argument = call.arguments();
        if (null == argument || null == mBaiduMap) {
            result.success(null);
            return;
        }
        if (!argument.containsKey("rect")) {
            result.success(null);
            return;
        }
        Map<String, Object> rect = (Map<String, Object>) argument.get("rect");

        WinRound winRound = FlutterDataConveter.bmfRectToWinRound(rect);
        if (null == winRound) {
            result.success(null);
            return;
        }

        if (winRound.left > winRound.right || winRound.top > winRound.bottom) {
            result.success(null);
            return;
        }

        if (winRound.right - winRound.left > getMapViewWidth()
                || winRound.bottom - winRound.top > getMapViewHeight()) {
            result.success(null);
            return;
        }

        // ??????????????????left <= right top <= bottom ??????????????????
        Rect recta = new Rect(winRound.left, winRound.top, winRound.right, winRound.bottom);
        mBaiduMap.snapshotScope(recta, new BaiduMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                if (null == bitmap) {
                    result.success(null);
                    return;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                result.success(byteArrayOutputStream.toByteArray());
            }
        });
    }

    private int getMapViewWidth() {
        if (mFlutterMapViewWrapper != null) {
            return mFlutterMapViewWrapper.getWidth();
        }
        return 0;
    }

    private int getMapViewHeight() {
        if (mFlutterMapViewWrapper != null) {
            return mFlutterMapViewWrapper.getHeight();
        }
        return 0;
    }

    /**
     * ?????? ????????????????????????
     */
    private void mapSnapshot(final MethodChannel.Result result) {
        if (null == mBaiduMap) {
            result.success(null);
            return;
        }
        mBaiduMap.snapshot(new BaiduMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap bitmap) {
                if (null == bitmap) {
                    result.success(null);
                    return;
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                result.success(byteArrayOutputStream.toByteArray());
            }
        });
    }

    /**
     * ????????????
     */
    private void setMapUpdate(MethodCall call, MethodChannel.Result result) {
        Map<String, Object> argument = call.arguments();
        if (null == argument || null == mBaiduMap) {
            result.success(false);
            return;
        }

        boolean ret = updateMapState(argument);

        result.success(ret);
    }

    /**
     * ?????????????????????????????????????????????
     */
    private void setNewCoordinateBounds(MethodCall call, MethodChannel.Result result) {
        Map<String, Object> argument = call.arguments();
        if (null == argument || null == mBaiduMap) {
            result.success(false);
            return;
        }

        if (!argument.containsKey("visibleMapBounds")) {
            result.success(false);
            return;
        }

        Map<String, Object> visibleMapBounds =
                (Map<String, Object>) argument.get("visibleMapBounds");
        if (null == visibleMapBounds) {
            result.success(false);
            return;
        }

        LatLngBounds latLngBounds = visibleMapBoundsImp(visibleMapBounds);
        if (null == latLngBounds) {
            result.success(false);
            return;
        }

        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngBounds(latLngBounds));
        result.success(true);
    }

    private LatLngBounds visibleMapBoundsImp(Map<String, Object> visibleMapBounds) {
        if (!visibleMapBounds.containsKey("northeast") || !visibleMapBounds
                .containsKey("southwest")) {
            return null;
        }
        HashMap<String, Double> northeast =
                (HashMap<String, Double>) visibleMapBounds.get("northeast");
        HashMap<String, Double> southwest =
                (HashMap<String, Double>) visibleMapBounds.get("southwest");
        if (null == northeast || null == southwest) {
            return null;
        }
        if (!northeast.containsKey("latitude") || !northeast.containsKey("longitude")
                || !southwest.containsKey("latitude") || !southwest.containsKey("longitude")) {
            return null;
        }

        Double northeastLatitude = northeast.get("latitude");
        Double northeastLongitude = northeast.get("longitude");
        Double southwestLatitude = southwest.get("latitude");
        Double southwestLongitude = southwest.get("longitude");

        if (null == northeastLatitude || null == northeastLongitude
                || null == southwestLatitude || null == southwestLongitude) {
            return null;
        }

        LatLng northeastLatLng = new LatLng(northeastLatitude, northeastLongitude);
        LatLng southwestLatLng = new LatLng(southwestLatitude, southwestLongitude);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(northeastLatLng);
        builder.include(southwestLatLng);

        return builder.build();
    }

    /**
     * ??????Padding???????????????????????????????????????
     */
    private void setVisibleMapBoundsWithPaddingMethod(MethodCall call,
                                                      MethodChannel.Result result) {
        Map<String, Object> argument = call.arguments();
        if (null == argument || null == mBaiduMap) {
            result.success(false);
            return;
        }

        if (!argument.containsKey("visibleMapBounds") || !argument.containsKey("insets")) {
            result.success(false);
            return;
        }

        Map<String, Object> visibleMapBounds =
                (Map<String, Object>) argument.get("visibleMapBounds");
        Map<String, Double> insets = (Map<String, Double>) argument.get("insets");
        if (null == visibleMapBounds || null == insets) {
            result.success(false);
            return;
        }

        LatLngBounds latLngBounds = visibleMapBoundsImp(visibleMapBounds);
        if (null == latLngBounds) {
            result.success(false);
            return;
        }

        if (!insets.containsKey("left") || !insets.containsKey("top")
                || !insets.containsKey("right") || !insets.containsKey("bottom")) {
            result.success(false);
            return;
        }
        Double left = insets.get("left");
        Double top = insets.get("top");
        Double right = insets.get("right");
        Double bottom = insets.get("bottom");

        if (null == left || null == top || null == right || null == bottom) {
            result.success(false);
            return;
        }
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(latLngBounds,
                left.intValue(), top.intValue(), right.intValue(), bottom.intValue());
        mBaiduMap.setMapStatus(mapStatusUpdate);
        result.success(true);
    }

    private void updateMap() {
        MapStatus.Builder builder = new MapStatus.Builder();
        MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newMapStatus(builder.build());
        mBaiduMap.setMapStatus(mapStatusUpdate);
    }

    public boolean updateMapState(Map<String, Object> mapOptionsMap) {
        if (null == mapOptionsMap || null == mMapController || mFlutterMapViewWrapper == null) {
            return false;
        }

        // 3.2.0 ????????????????????????????????????
        Boolean showOperateLayerEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "showOperateLayer");
        if (null != showOperateLayerEnabled) {
            mMapController.showOperateLayer(showOperateLayerEnabled);
            BMFMapStatus.getsInstance().setShowOperateLayerEnable(showOperateLayerEnabled);
        }


        // 3.1.0?????? ????????????????????????
        Integer languageType = new TypeConverter<Integer>().getValue(mapOptionsMap, "languageType");
        if (null != languageType) {
            mMapController.setMapLanguage(languageType);
        }

        // 3.1.0?????? ????????????????????????
        Integer fontSizeLevel = new TypeConverter<Integer>().getValue(mapOptionsMap, "fontSizeLevel");
        if (null != fontSizeLevel) {
            mMapController.setFontSizeLevel(fontSizeLevel);
        }

        // ??????????????????
        Integer mapType = new TypeConverter<Integer>().getValue(mapOptionsMap, "mapType");
        if (null != mapType) {
            // flutter????????????????????????SDK?????????????????????0??????BaiduMap.MAP_TYPE_NONE???
            if (mapType == 0) {
                mapType = BaiduMap.MAP_TYPE_NONE;
            }
            mMapController.setMapType(mapType);
        }

        // ???????????????????????????
        Map<String, Object> compassPosMap =
                new TypeConverter<Map<String, Object>>().getValue(mapOptionsMap, "compassPosition");
        Point compassPos = FlutterDataConveter.mapToPoint(compassPosMap);
        if (null != compassPos) {
            mBaiduMap.setCompassPosition(compassPos);
        }

        updateMapStatus(mapOptionsMap);

        // ????????????????????????
        Map<String, Object> visibleMapBounds = new TypeConverter<Map<String, Object>>()
                .getValue(mapOptionsMap, "visibleMapBounds");
        LatLngBounds latLngBounds = FlutterDataConveter.mapToLatlngBounds(visibleMapBounds);
        if (null != latLngBounds) {
            MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngBounds(latLngBounds);
            mBaiduMap.setMapStatus(mapStatusUpdate);
        }

        // ???????????????????????????????????????
        Integer minZoomLevel = new TypeConverter<Integer>().getValue(mapOptionsMap, "minZoomLevel");
        Integer maxZoomLevel = new TypeConverter<Integer>().getValue(mapOptionsMap, "maxZoomLevel");
        if (null != minZoomLevel && null != maxZoomLevel) {
            mMapController
                    .setMaxAndMinZoomLevel(maxZoomLevel.floatValue(), minZoomLevel.floatValue());
        } else if (null == minZoomLevel && null != maxZoomLevel) {
            mMapController
                    .setMaxAndMinZoomLevel(maxZoomLevel.floatValue(), mBaiduMap.getMinZoomLevel());
        } else if (null != minZoomLevel && null == maxZoomLevel) {
            mMapController
                    .setMaxAndMinZoomLevel(mBaiduMap.getMaxZoomLevel(), minZoomLevel.floatValue());
        }

        // ??????????????????????????????
        Boolean showZoomControl = new TypeConverter<Boolean>().getValue(mapOptionsMap,
                "showZoomControl");
        if (null != showZoomControl) {
            mFlutterMapViewWrapper.showZoomControl(showZoomControl);
        }

        // ????????????3d?????????
        Boolean buildingsEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "buildingsEnabled");
        if (null != buildingsEnabled) {
            mMapController.setBuildingsEnabled(buildingsEnabled);
        }

        // ??????????????????poi??????
        Boolean showMapPoi = new TypeConverter<Boolean>().getValue(mapOptionsMap, "showMapPoi");
        if (null != showMapPoi) {
            mMapController.showMapPoi(showMapPoi);
        }

        // ??????????????????????????????
        Boolean trafficEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "trafficEnabled");
        if (null != trafficEnabled) {
            mMapController.setTrafficEnabled(trafficEnabled);
        }

        // ???????????????????????????
        if (mapOptionsMap.containsKey("limitMapBounds")) {
            Map<String, Object> limitMapRegion =
                    (Map<String, Object>) mapOptionsMap.get("limitMapBounds");
            if (null != limitMapRegion) {
                LatLngBounds limitLatLngBounds =
                        FlutterDataConveter.mapToLatlngBounds(limitMapRegion);
                if (null != limitLatLngBounds) {
                    mMapController.setMapStatusLimits(limitLatLngBounds);
                }
            }
        }

        // ???????????????????????????????????????
        Boolean baiduHeatMapEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "baiduHeatMapEnabled");
        if (null != baiduHeatMapEnabled) {
            mMapController.setBaiduHeatMapEnabled(baiduHeatMapEnabled);
        }

        // ????????????????????????
        Boolean gesturesEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "gesturesEnabled");
        if (null != gesturesEnabled) {
            mMapController.setAllGesturesEnabled(gesturesEnabled);
        }

        // ??????????????????????????????
        Boolean zoomEnabled = new TypeConverter<Boolean>().getValue(mapOptionsMap, "zoomEnabled");
        if (null != zoomEnabled) {
            mMapController.setZoomGesturesEnabled(zoomEnabled);
        }

        // ???????????????????????????
        Boolean scrollEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "scrollEnabled");
        if (null != scrollEnabled) {
            mMapController.setScrollGesturesEnabled(scrollEnabled);
        }

        // ???????????????????????????
        Boolean overlookEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "overlookEnabled");
        if (null != overlookEnabled) {
            mMapController.setOverlookingGesturesEnabled(overlookEnabled);
        }

        // ???????????????????????????
        Boolean rotateEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "rotateEnabled");
        if (null != rotateEnabled) {
            mMapController.setRotateGesturesEnabled(rotateEnabled);
        }

        // ???????????????????????????
        Boolean showMapScaleBar =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "showMapScaleBar");
        if (null != showMapScaleBar) {
            mMapController.showScaleControl(showMapScaleBar);
        }

        // ???????????????????????????
        Map<String, Object> mapScaleBarPosMap = new TypeConverter<Map<String, Object>>()
                .getValue(mapOptionsMap, "mapScaleBarPosition");
        Point mapScaleBarPos = FlutterDataConveter.mapToPoint(mapScaleBarPosMap);
        if (null != mapScaleBarPos) {
            mMapController.setScaleControlPosition(mapScaleBarPos);
        }

        // ????????????logo????????????
        Integer logoPosition = new TypeConverter<Integer>().getValue(mapOptionsMap, "logoPosition");
        if (null != logoPosition
                && logoPosition >= LogoPosition.logoPostionleftBottom.ordinal()
                && logoPosition <= LogoPosition.logoPostionRightTop.ordinal()) {
            mMapController.setLogoPosition(LogoPosition.values()[logoPosition.intValue()]);
        }

        // ????????????padding
        Map<String, Double> mapPadding =
                new TypeConverter<Map<String, Double>>().getValue(mapOptionsMap, "mapPadding");
        if (null != mapPadding) {
            if (mapPadding.containsKey("top") && mapPadding.containsKey("left")
                    && mapPadding.containsKey("bottom") && mapPadding.containsKey("right")) {
                Double top = mapPadding.get("top");
                Double left = mapPadding.get("left");
                Double bottom = mapPadding.get("bottom");
                Double right = mapPadding.get("right");

                if (top != null && left != null && bottom != null && right != null) {
                    int iTop = top.intValue();
                    int iLeft = left.intValue();
                    int iBottom = bottom.intValue();
                    int iRight = right.intValue();
                    mMapController.setViewPadding(iLeft, iTop, iRight, iBottom);
                }
            }
        }

        // ?????????????????????????????????????????????????????????????????????????????????
        Boolean changeCenterWithDoubleTouchPointEnabled = new TypeConverter<Boolean>()
                .getValue(mapOptionsMap, "changeCenterWithDoubleTouchPointEnabled");
        if (null != changeCenterWithDoubleTouchPointEnabled) {
            // ????????????sdk??????????????????????????????????????????
            mMapController.setEnlargeCenterWithDoubleClickEnable(
                    !changeCenterWithDoubleTouchPointEnabled);
        }

        // ???????????????????????????
        Boolean baseIndoorMapEnabled =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "baseIndoorMapEnabled");
        if (null != baseIndoorMapEnabled) {
            mMapController.setIndoorEnable(baseIndoorMapEnabled);
            BMFMapStatus.getsInstance().setBaseIndoorEnable(baseIndoorMapEnabled);
        }

        // ???????????????????????????poi
        Boolean showIndoorMapPoi =
                new TypeConverter<Boolean>().getValue(mapOptionsMap, "showIndoorMapPoi");
        if (null != showIndoorMapPoi) {
            mMapController.showMapIndoorPoi(showIndoorMapPoi);
            BMFMapStatus.getsInstance().setIndoorMapPoiEnable(showIndoorMapPoi);
        }

        return true;
    }

    private void updateMapStatus(Map<String, Object> mapOptionsMap) {
        if (mBaiduMap == null) {
            return;
        }
        boolean isNeedUpdate = false;
        MapStatus.Builder builder = new MapStatus.Builder(mBaiduMap.getMapStatus());
        // ?????????????????????
        Map<String, Object> centerMap =
                new TypeConverter<Map<String, Object>>().getValue(mapOptionsMap, "center");
        LatLng center = FlutterDataConveter.mapToLatlng(centerMap);
        if (null != center) {
            isNeedUpdate = true;
            builder.target(center);
        }

        // ????????????????????????
        Double rotation = new TypeConverter<Double>().getValue(mapOptionsMap, "rotation");
        if (null != rotation) {
            isNeedUpdate = true;
            builder.rotate(rotation.floatValue());
        }

        // ????????????????????????
        if (mapOptionsMap.containsKey("overlooking")) {
            Double overlooking = (Double) mapOptionsMap.get("overlooking");
            if (overlooking != null) {
                isNeedUpdate = true;
                builder.overlook(overlooking.floatValue());
            }
        }

        // ????????????????????????
        Integer zoomLevel = new TypeConverter<Integer>().getValue(mapOptionsMap, "zoomLevel");
        if (null != zoomLevel) {
            isNeedUpdate = true;
            builder.zoom(zoomLevel);
        }

        // ???????????????????????????????????????????????????.
        if (isNeedUpdate) {
            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    }

}
