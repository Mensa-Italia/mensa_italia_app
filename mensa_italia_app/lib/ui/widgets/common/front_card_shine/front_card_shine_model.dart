import 'dart:async';

import 'package:gyro_provider/models/vector_model.dart';
import 'package:stacked/stacked.dart';

class FrontCardShineModel extends BaseViewModel {
  double normalizedRotation = 0.5;
  Timer? _timer;
  bool _isResetting = false;

  void calculateRotation(VectorModel rotation) {
    final oldNormalizedRotation = normalizedRotation;
    normalizedRotation += ((rotation.y / 10) / 2);
    normalizedRotation = normalizedRotation.clamp(0, 1);

    if ((oldNormalizedRotation - normalizedRotation).abs() > 0.01) {
      _isResetting = false;
      if (_timer != null) {
        _timer!.cancel();
      }

      _timer = Timer(const Duration(seconds: 5), () async {
        _isResetting = true;
        final step = (normalizedRotation - 0.5).abs() / 100;
        for (var i = 0; i < 100; i++) {
          if (!_isResetting) {
            break;
          }
          await Future.delayed(const Duration(milliseconds: 10));
          if (normalizedRotation > 0.5) {
            if (_isResetting) {
              normalizedRotation -= step;
            }
          } else {
            if (_isResetting) {
              normalizedRotation += step;
            }
          }
          rebuildUi();
        }
        if (_isResetting) {
          normalizedRotation = 0.5;
        }
        rebuildUi();
      });
    }
  }
}
