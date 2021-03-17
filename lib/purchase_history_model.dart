class PurchaseHistoryModel {
  String orderId;
  String packageName;
  String productId;
  int purchaseTime;
  int purchaseState;
  String purchaseToken;
  bool autoRenewing;
  bool acknowledged;

  PurchaseHistoryModel(
      {this.orderId,
        this.packageName,
        this.productId,
        this.purchaseTime,
        this.purchaseState,
        this.purchaseToken,
        this.autoRenewing,
        this.acknowledged});

  PurchaseHistoryModel.fromJson(Map<String, dynamic> json) {
    orderId = json['orderId'];
    packageName = json['packageName'];
    productId = json['productId'];
    purchaseTime = json['purchaseTime'];
    purchaseState = json['purchaseState'];
    purchaseToken = json['purchaseToken'];
    autoRenewing = json['autoRenewing'];
    acknowledged = json['acknowledged'];
  }

  Map<String, dynamic> toJson() {
    final Map<String, dynamic> data = new Map<String, dynamic>();
    data['orderId'] = this.orderId;
    data['packageName'] = this.packageName;
    data['productId'] = this.productId;
    data['purchaseTime'] = this.purchaseTime;
    data['purchaseState'] = this.purchaseState;
    data['purchaseToken'] = this.purchaseToken;
    data['autoRenewing'] = this.autoRenewing;
    data['acknowledged'] = this.acknowledged;
    return data;
  }
}