import 'package:flutter/material.dart';
import 'package:mensa_italia_app/api/api.dart';
import 'package:mensa_italia_app/model/notification.dart';
import 'package:mensa_italia_app/model/receipt.dart';
import 'package:pocketbase/pocketbase.dart';

class ReceiptSSE extends ChangeNotifier {
  static final ReceiptSSE _instance = ReceiptSSE._internal();
  factory ReceiptSSE() => _instance;
  ReceiptSSE._internal();

  UnsubscribeFunc? _unsubscribe;

  List<ReceiptModel> _data = [];

  List<ReceiptModel> get receipts => _data;

  void start() {
    Api().pb.collection('payments').subscribe('*', (e) {
      if (e.action == "create") {
        _data.add(ReceiptModel.fromJson(e.record!.toJson()));
      } else if (e.action == "update") {
        var index = _data.indexWhere((element) => element.id == e.record!.id);
        if (index != -1) {
          _data[index] = ReceiptModel.fromJson(e.record!.toJson());
        } else {
          _data.add(ReceiptModel.fromJson(e.record!.toJson()));
        }
      } else if (e.action == "delete") {
        _data.removeWhere((element) => element.id == e.record!.id);
      }
      _data.sort((a, b) => b.created.compareTo(a.created));
      notifyListeners();
    }).then((value) {
      _unsubscribe = value;
      Api().getPaymentsReceipt().then((value) {
        _data.addAll(value);
        _data.sort((a, b) => b.created.compareTo(a.created));
        notifyListeners();
      });
    });
  }

  void stop() {
    _unsubscribe?.call();
  }
}
