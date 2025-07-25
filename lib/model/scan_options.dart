import '../gen/protos/protos.pb.dart';
import '../model/android_options.dart';

/// Provides options to configure the barcode scanner
class ScanOptions {
  /// Create a object which represents the scanner options
  const ScanOptions({
    this.restrictFormat = const [],
    this.useCamera = -1,
    this.android = const AndroidOptions(),
    this.autoEnableFlash = false,
    this.strings = const {
      'cancel': 'Cancel',
      'flash_on': 'Flash on',
      'flash_off': 'Flash off',
    },
    this.withImage = false,
  }) : assert(useCamera >= -1);

  /// This map contains strings which are displayed to the user
  ///
  /// Possible pairs:
  /// - cancel : The text of the cancel button (iOS only)
  //  - flash_on : The text of the flash on button
  //  - flash_off : The Text of the flash off button
  final Map<String, String> strings;
  // capture an image after a barcode is detected
  final bool withImage;

  /// Restrict the supported barcode formats
  final List<BarcodeFormat> restrictFormat;

  /// Index of the camera which should used. -1 uses the default camera
  final int useCamera;

  /// Android specific configuration
  final AndroidOptions android;

  /// Set to true to automatically enable flash on camera start
  final bool autoEnableFlash;
}
