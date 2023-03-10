//
//  BMFMapViewController.m
//  flutter_baidu_mapapi_map
//
//  Created by zbj on 2020/2/6.
//

#import "BMFMapViewController.h"
#import <flutter_baidu_mapapi_base/BMFMapModels.h>
#import <flutter_baidu_mapapi_base/UIColor+BMFString.h>
#import <flutter_baidu_mapapi_base/NSObject+BMFThread.h>
#import <flutter_baidu_mapapi_base/BMFDefine.h>

#import "BMFMapView.h"
#import "BMFMapCallBackConst.h"
#import "BMFMapViewHandles.h"
#import "BMFAnnotationHandles.h"
#import "BMFOverlayHandles.h"
#import "BMFHeatMapHandles.h"
#import "BMFUserLocationHandles.h"
#import "BMFProjectionHandles.h"
#import "BMFMapStatusModel.h"
#import "BMFMapPoiModel.h"
#import "BMFIndoorMapInfoModel.h"

#import "BMFAnnotationViewManager.h"
#import "BMFAnnotationModel.h"

#import "BMFOverlayViewManager.h"
#import "BMFPolylineModel.h"

static NSString * const kBMFMapChannelName = @"flutter_bmfmap/map_";
static NSString * const kMapMethods = @"flutter_bmfmap/map/";
static NSString * const kMarkerMethods = @"flutter_bmfmap/marker/";
static NSString * const kOverlayMethods = @"flutter_bmfmap/overlay/";
static NSString * const kHeatMapMethods = @"flutter_bmfmap/heatMap/";
static NSString * const kUserLocationMethods = @"flutter_bmfmap/userLocation/";
static NSString * const kProjectionMethods = @"flutter_bmfmap/projection/";


@interface BMFMapViewController()<BMKMapViewDelegate>
{
    FlutterMethodChannel *_channel;
    BMFMapView  *_mapView;
}

@end
@implementation BMFMapViewController

- (instancetype)initWithWithFrame:(CGRect)frame
                   viewIdentifier:(int64_t)viewId
                        arguments:(id _Nullable)args
                  binaryMessenger:(NSObject<FlutterBinaryMessenger> *)messenger {
    if ([super init]) {
        int Id = (int)(viewId);
        NSString *channelName = [NSString stringWithFormat:@"%@%@", kBMFMapChannelName, [NSString stringWithFormat:@"%d", Id]];
        _channel = [FlutterMethodChannel methodChannelWithName:channelName binaryMessenger:messenger];
        _mapView = [BMFMapView viewWithFrame:frame dic:(NSDictionary *)args];
        _mapView.delegate = self;
        
#pragma mark - flutter -> ios
        __weak __typeof__(_mapView) weakMapView = _mapView;
        __weak __typeof__(_channel) weakChannel = _channel;
        [_channel setMethodCallHandler:^(FlutterMethodCall * _Nonnull call, FlutterResult  _Nonnull result) {
            NSObject<BMFMapViewHandler> *handler;
            
            if ([call.method hasPrefix:kMapMethods]) { // map
                handler = [NSClassFromString([BMFMapViewHandles defalutCenter].mapViewHandles[call.method]) new];
            }
            else if ([call.method hasPrefix:kMarkerMethods]) { // marker
                handler = [NSClassFromString([BMFAnnotationHandles defalutCenter].annotationHandles[call.method]) new];
            }
            else if ([call.method hasPrefix:kOverlayMethods]) {  // overlay
                handler = [NSClassFromString([BMFOverlayHandles defalutCenter].overlayHandles[call.method]) new];
            }
            else  if ([call.method hasPrefix:kHeatMapMethods]) { // ?????????
                
                handler = [NSClassFromString([BMFHeatMapHandles defalutCenter].heatMapHandles[call.method]) new];
                [[BMFHeatMapHandles defalutCenter].handlerArray addObject:handler];
            }
            else  if ([call.method hasPrefix:kUserLocationMethods]) { // ????????????
                handler = [NSClassFromString([BMFUserLocationHandles defalutCenter].userLocationHandles[call.method]) new];
            }
            else  if ([call.method hasPrefix:kProjectionMethods]) { // ????????????
                handler = [NSClassFromString([BMFProjectionHandles defalutCenter].projectionHandles[call.method]) new];
            }
//            NSLog(@"call.method = %@", call.method);
            
            if (handler) {
                [weakMapView bmf_performBlockOnMainThreadAsync:^{
                    if ([handler respondsToSelector:@selector(initWith:channel:)]) {
                        [[handler initWith:weakMapView channel:weakChannel] handleMethodCall:call result:result];
                    } else {
                        [[handler initWith:weakMapView] handleMethodCall:call result:result];
                    }
                }];
            } else {
                
//                if ([call.method isEqualToString:@"flutter_bmfmap/map/didUpdateWidget"]) {
//                    NSLog(@"native - didUpdateWidget");
//                    return;
//                }
//                if ([call.method isEqualToString:@"flutter_bmfmap/map/reassemble"]) {
//                    NSLog(@"native - reassemble");
//                    return;
//                }
                result(FlutterMethodNotImplemented);
            }
        }];
    }
    return self;
}

- (nonnull UIView *)view {
    return _mapView;
}

- (void)dealloc {
    _channel = nil;
    _mapView.delegate = nil;
    _mapView = nil;
    //    NSLog(@"-BMFMapViewController-dealloc");
}

#pragma mark - ios -> flutter
#pragma mark - BMKMapViewDelegate
/// ??????????????????
- (void)mapViewDidFinishLoading:(BMKMapView *)mapView {
    if (_mapView) {
        // ??????????????????????????????????????????????????????.??????????????????
        [_mapView updateMapInitOptions];
    }
    if (!_channel) return;
    [_channel invokeMethod:kBMFMapDidLoadCallback arguments:@{@"success": @YES} result:nil];
}
/// ??????????????????
- (void)mapViewDidFinishRendering:(BMKMapView *)mapView {
    if (!_channel) return;
    [_channel invokeMethod:kBMFMapDidRenderCallback arguments:@{@"success": @YES} result:nil];
}

/// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
- (void)mapView:(BMKMapView *)mapView onDrawMapFrame:(BMKMapStatus*)status {
    if (!_channel) return;
    BMFMapStatusModel *mapStatus = [BMFMapStatusModel fromMapStatus:status];
    [_channel invokeMethod:kBMFMapOnDrawMapFrameCallback
                 arguments:@{@"mapStatus": [mapStatus bmf_toDictionary]}
                    result:nil];
    
}

/// ?????????????????????????????????????????????
- (void)mapView:(BMKMapView *)mapView regionWillChangeAnimated:(BOOL)animated {
    if (!_channel) return;
    BMFMapStatusModel *mapStatus = [BMFMapStatusModel fromMapStatus:[_mapView getMapStatus]];
    [_channel invokeMethod:kBMFMapRegionWillChangeCallback
                 arguments:@{@"mapStatus": [mapStatus bmf_toDictionary]}
                    result:nil];
}

/// ?????????????????????????????????????????????
- (void)mapView:(BMKMapView *)mapView regionWillChangeAnimated:(BOOL)animated reason:(BMKRegionChangeReason)reason {
    if (!_channel) return;
    BMFMapStatusModel *mapStatus = [BMFMapStatusModel fromMapStatus:[_mapView getMapStatus]];
    [_channel invokeMethod:kBMFMapRegionWillChangeWithReasonCallback
                 arguments:@{@"mapStatus": [mapStatus bmf_toDictionary], @"reason": @(reason)}
                    result:nil];
}

/// ?????????????????????????????????????????????
- (void)mapView:(BMKMapView *)mapView regionDidChangeAnimated:(BOOL)animated {
    if (!_channel) return;
    BMFMapStatusModel *mapStatus = [BMFMapStatusModel fromMapStatus:[_mapView getMapStatus]];
    [_channel invokeMethod:kBMFMapRegionDidChangeCallback
                 arguments:@{@"mapStatus": [mapStatus bmf_toDictionary]}
                    result:nil];
}

/// ?????????????????????????????????????????????
- (void)mapView:(BMKMapView *)mapView regionDidChangeAnimated:(BOOL)animated reason:(BMKRegionChangeReason)reason {
    if (!_channel) return;
    BMFMapStatusModel *mapStatus = [BMFMapStatusModel fromMapStatus:[_mapView getMapStatus]];
    [_channel invokeMethod:kBMFMapRegionDidChangeWithReasonCallback
                 arguments:@{@"mapStatus": [mapStatus bmf_toDictionary], @"reason": @(reason)}
                    result:nil];
}
/// ???????????????????????????????????????
- (void)mapView:(BMKMapView *)mapView onClickedMapPoi:(BMKMapPoi *)mapPoi {
    if (!_channel) return;
    BMFMapPoiModel *model = [BMFMapPoiModel fromBMKMapPoi:mapPoi];
    [_channel invokeMethod:kBMFMapOnClickedMapPoiCallback arguments:@{@"poi": [model bmf_toDictionary]} result:nil];
}
/// ???????????????????????????????????????
- (void)mapView:(BMKMapView *)mapView onClickedMapBlank:(CLLocationCoordinate2D)coordinate {
    if (!_channel) return;
    BMFCoordinate *coord = [BMFCoordinate fromCLLocationCoordinate2D:coordinate];
    [_channel invokeMethod:kBMFMapOnClickedMapBlankCallback arguments:@{@"coord": [coord bmf_toDictionary]} result:nil];
}

/// ?????????????????????????????????
- (void)mapview:(BMKMapView *)mapView onDoubleClick:(CLLocationCoordinate2D)coordinate {
    if (!_channel) return;
    BMFCoordinate *coord = [BMFCoordinate fromCLLocationCoordinate2D:coordinate];
    [_channel invokeMethod:kBMFMapOnDoubleClickCallback arguments:@{@"coord": [coord bmf_toDictionary]} result:nil];
}

/// ?????????????????????????????????
- (void)mapview:(BMKMapView *)mapView onLongClick:(CLLocationCoordinate2D)coordinate {
    if (!_channel) return;
    BMFCoordinate *coord = [BMFCoordinate fromCLLocationCoordinate2D:coordinate];
    [_channel invokeMethod:kBMFMapOnLongClickCallback arguments:@{@"coord": [coord bmf_toDictionary]} result:nil];
}

/// 3DTouch ?????????????????????????????????????????????3D Touch??????fouchTouchEnabled?????????YES???????????????????????????
/// force ?????????????????????(??????UITouch???force??????)
/// maximumPossibleForce ??????????????????????????????????????????(??????UITouch???maximumPossibleForce??????)
- (void)mapview:(BMKMapView *)mapView onForceTouch:(CLLocationCoordinate2D)coordinate force:(CGFloat)force maximumPossibleForce:(CGFloat)maximumPossibleForce {
    if (!_channel) return;
    BMFCoordinate *coord = [BMFCoordinate fromCLLocationCoordinate2D:coordinate];
    [_channel invokeMethod:kBMFMapOnForceTouchCallback arguments:@{@"coord": [coord bmf_toDictionary], @"force": @(force), @"maximumPossibleForce": @(maximumPossibleForce)} result:nil];
}

/// ?????????????????????????????????????????????
- (void)mapStatusDidChanged:(BMKMapView *)mapView {
    if (!_channel) return;
    [_channel invokeMethod:kBMFMapStatusDidChangedCallback arguments:nil result:nil];
}

- (void)mapview:(BMKMapView *)mapView baseIndoorMapWithIn:(BOOL)flag baseIndoorMapInfo:(BMKBaseIndoorMapInfo *)info {
    if (!_channel) return;
    BMFIndoorMapInfoModel *model = [BMFIndoorMapInfoModel new];
    model.strID = info.strID;
    model.strFloor = info.strFloor;
    model.listStrFloors = [info.arrStrFloors copy];
    [_channel invokeMethod:kBMFMapInOrOutBaseIndoorMapCallback arguments:@{@"flag": @(flag), @"info": [model bmf_toDictionary]} result:nil];
}
#pragma mark - annotationView
/// ??????anntation???????????????View
- (__kindof BMKAnnotationView *)mapView:(BMKMapView *)mapView viewForAnnotation:(id<BMKAnnotation>)annotation {
    return [BMFAnnotationViewManager mapView:mapView viewForAnnotation:annotation];
}

/// ???mapView?????????annotation views?????????????????????
- (void)mapView:(BMKMapView *)mapView didAddAnnotationViews:(NSArray *)views {
    if (!_channel) return;
}

/// ????????????BMKAnnotationView????????????????????????
- (void)mapView:(BMKMapView *)mapView clickAnnotationView:(BMKAnnotationView *)view {
    if (!_channel) return;
    if ([view isKindOfClass:NSClassFromString(@"BMKUserLocationView")]) {
        return;
    }
    // ??????marker??????model
    BMFAnnotationModel *model = [BMFAnnotationViewManager annotationModelfromAnnotionView:view];
    [_channel invokeMethod:kBMFMapClickedMarkerCallback arguments:@{@"marker": [model bmf_toDictionary]} result:nil];
}
/// ???????????????annotation views?????????????????????
/// @param mapView ??????View
/// @param view ?????????annotation views
- (void)mapView:(BMKMapView *)mapView didSelectAnnotationView:(BMKAnnotationView *)view {
    if (!_channel) return;
    if ([view isKindOfClass:NSClassFromString(@"BMKUserLocationView")]) {
        return;
    }
    // ??????marker??????model
    BMFAnnotationModel *model = [BMFAnnotationViewManager annotationModelfromAnnotionView:view];
    model.selected = view.selected;
    [_channel invokeMethod:kBMFMapDidSelectMarkerCallback arguments:@{@"marker": [model bmf_toDictionary]} result:nil];
}

/// ?????????????????????annotationView?????????????????????
- (void)mapView:(BMKMapView *)mapView didDeselectAnnotationView:(BMKAnnotationView *)view {
    if (!_channel) return;
    if ([view isKindOfClass:NSClassFromString(@"BMKUserLocationView")]) {
        return;
    }
    // ??????marker??????model
    BMFAnnotationModel *model = [BMFAnnotationViewManager annotationModelfromAnnotionView:view];
    model.selected = view.selected;
    [_channel invokeMethod:kBMFMapDidDeselectMarkerCallback arguments:@{@"marker": [model bmf_toDictionary]} result:nil];
}

/// ??????annotation view?????????view?????????????????????????????????????????????ios3.2????????????
- (void)mapView:(BMKMapView *)mapView annotationView:(BMKAnnotationView *)view didChangeDragState:(BMKAnnotationViewDragState)newState
   fromOldState:(BMKAnnotationViewDragState)oldState {
    if (!_channel) return;
    // ??????marker??????model
    BMFAnnotationModel *model = [BMFAnnotationViewManager annotationModelfromAnnotionView:view];
    model.position = [BMFCoordinate fromCLLocationCoordinate2D:view.annotation.coordinate];
    [_channel invokeMethod:kBMFMapDidDragMarkerCallback
                 arguments:@{@"marker" : [model bmf_toDictionary],
                             @"newState" : @(newState),
                             @"oldState" : @(oldState)
                 }
                    result:nil];
    
}

/// ?????????annotationView?????????view?????????????????????
- (void)mapView:(BMKMapView *)mapView annotationViewForBubble:(BMKAnnotationView *)view {
    if (!_channel) return;
    // ??????marker??????model
    BMFAnnotationModel *model = [BMFAnnotationViewManager annotationModelfromAnnotionView:view];
    [_channel invokeMethod:kBMFMapDidClickedPaoPaoCallback arguments:@{@"marker": [model bmf_toDictionary]} result:nil];
}

#pragma mark - overlayView

- (__kindof BMKOverlayView *)mapView:(BMKMapView *)mapView viewForOverlay:(id<BMKOverlay>)overlay {
    if ([overlay isKindOfClass:[BMKTraceOverlay class]] ||
        [overlay isKindOfClass:[BMKMultiPointOverlay class]]) {
        [BMFOverlayViewManager defalutCenter].channel = _channel;
    }
    return [BMFOverlayViewManager mapView:mapView viewForOverlay:overlay];
}

/// ???mapView?????????overlay views?????????????????????
/// @param mapView ??????View
/// @param overlayViews ????????????overlay views
- (void)mapView:(BMKMapView *)mapView didAddOverlayViews:(NSArray *)overlayViews {
    if (!_channel) return;
    
    // TODO: didAddOverlayViews
}

/// ????????????????????????????????????????????????????????????BMKPolylineView?????????
/// @param mapView ??????View
/// @param overlayView ?????????view??????
- (void)mapView:(BMKMapView *)mapView onClickedBMKOverlayView:(BMKOverlayView *)overlayView {
    if (!_channel) return;
    
    if ([overlayView isKindOfClass:[BMKPolylineView class]]) {
        BMFPolylineModel *model = [BMFOverlayViewManager polylineModelWith:(BMKPolylineView *)overlayView];
        //        NSLog(@"%@", [model bmf_toDictionary]);
        // ?????????polylineModel??????
        [_channel invokeMethod:kMapOnClickedOverlayCallback arguments:@{@"polyline": [model bmf_toDictionary]} result:nil];
    }
}


@end

@interface FlutterMapViewFactory()
{
    NSObject<FlutterBinaryMessenger> *_messenger;
}
@end

@implementation FlutterMapViewFactory

- (instancetype)initWithMessenger:(NSObject<FlutterBinaryMessenger> *)messager {
    if ([super init]) {
        _messenger = messager;
    }
    return self;
}

- (NSObject<FlutterMessageCodec> *)createArgsCodec {
    return [FlutterStandardMessageCodec sharedInstance];
}

- (NSObject<FlutterPlatformView> *)createWithFrame:(CGRect)frame viewIdentifier:(int64_t)viewId arguments:(id)args {
    BMFMapViewController *mapViewController = [[BMFMapViewController alloc] initWithWithFrame:frame viewIdentifier:viewId arguments:args binaryMessenger:_messenger];
    return mapViewController;
}



@end
