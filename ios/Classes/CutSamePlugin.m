#import "CutSamePlugin.h"
#if __has_include(<cut_same_plugin/cut_same_plugin-Swift.h>)
#import <cut_same_plugin/cut_same_plugin-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "cut_same_plugin-Swift.h"
#endif

@implementation CutSamePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftCutSamePlugin registerWithRegistrar:registrar];
}
@end
