class InternalPaymentMethod {
  final String id;
  final String brand;
  final String display;

  InternalPaymentMethod({
    required this.id,
    required this.brand,
    required this.display,
  });

  factory InternalPaymentMethod.fromJson(Map<String, dynamic> json) {
    return InternalPaymentMethod(
      id: json['id'],
      brand: _parseBrand(json),
      display: _parseDisplay(json),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'brand': brand,
      'display': display,
    };
  }

  static _parseBrand(Map<String, dynamic> json) {
    final typeOfPM = json['type'];
    if (typeOfPM == 'card') {
      return capitalize(json['card']['brand']);
    } else {
      switch (typeOfPM) {
        case "paypal":
          return "PayPal";
        case "sepa_debit":
          return "SEPA Direct Debit";
        case "revolut_pay":
          return "Revolut Pay";
        default:
          return "Unknown";
      }
    }
  }

  static _parseDisplay(Map<String, dynamic> json) {
    final typeOfPM = json['type'];
    if (typeOfPM == 'card') {
      return "**** **** **** ${json['card']['last4']}";
    } else {
      return "";
    }
  }

  static String capitalize(String s) {
    return s[0].toUpperCase() + s.substring(1);
  }

  String get image {
    switch (brand) {
      case "Visa":
        return "assets/cards/visa.png";
      case "Mastercard":
        return "assets/cards/mastercard.png";
      case "American Express":
        return "assets/cards/amex.png";
      case "Discover":
        return "assets/cards/discover.png";
      case "JCB":
        return "assets/cards/jcb.png";
      case "Diners Club":
        return "assets/cards/diners.png";
      case "UnionPay":
        return "assets/cards/unionpay.png";
      case "Revolut Pay":
        return "assets/cards/revolut.png";
      case "PayPal":
        return "assets/cards/paypal.png";
      case "SEPA Direct Debit":
        return "assets/cards/sepa.png";
      default:
        return "assets/cards/credit-card.png";
    }
  }
}
