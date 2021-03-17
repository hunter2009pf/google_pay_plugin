package com.taxen.google_pay_plugin

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar


/** GooglePayPlugin */
public class GooglePayPlugin: FlutterPlugin, MethodCallHandler, ActivityAware, PurchasesUpdatedListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  private lateinit var context: Context

  private  lateinit var activity: Activity

  private lateinit var billingClient: BillingClient

  private var id: String? = ""

  private lateinit var _result: Result

  private var isConsumedProduct: Boolean = true //是 消耗型商品 还是 订阅，这会决定确认购买交易的api调用

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "google_pay_plugin")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  // This static function is optional and equivalent to onAttachedToEngine. It supports the old
  // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
  // plugin registration via this function while apps migrate to use the new Android APIs
  // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
  //
  // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
  // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
  // depending on the user's project. onAttachedToEngine or registerWith must both be defined
  // in the same class.
  companion object {
    @JvmStatic
    fun registerWith(registrar: Registrar) {
      val channel = MethodChannel(registrar.messenger(), "google_pay_plugin")
      channel.setMethodCallHandler(GooglePayPlugin())
    }
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    _result = result
    if (call.method == "getPlatformVersion") {
      result.success("Android ${android.os.Build.VERSION.RELEASE}")
    } else if(call.method == "initGooglePayment"){
      initGooglePayment()
    } else if(call.method == "buyOneTimeConsumedProduct"){
      if(!isConnected()){
        result.error("google payment not connected", "", "")
      } else{
        isConsumedProduct = true
        id = call.argument<String>("id")
        querySkuDetails(id, true)
      }
    } else if(call.method == "subscribeService"){
      if(!isConnected()){
        result.error("google payment not connected", "", "")
      } else{
        isConsumedProduct = false
        id = call.argument<String>("id")
        querySkuDetails(id, false)
      }
    } else if(call.method == "verifySubscription"){
      if(!isConnected()){
        result.error("google payment not connected", "", "")
      } else{
        id = call.argument<String>("id")
        verifySubscription(id)
      }
    } else {
      result.notImplemented()
    }
  }

  private fun initGooglePayment(){
    billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
    billingClient.startConnection(object : BillingClientStateListener {
      override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
          _result.success("ok")
          // The BillingClient is ready. You can query purchases here.
//            queryPurchases("pay_98_use_forever")
//            querySkuDetails("weekly_member_58")
        } else {
          _result.error("google payment init code is ${billingResult.responseCode}", "fail to init google payment", "")
        }
      }

      override fun onBillingServiceDisconnected() {
        // Try to restart the connection on the next request to
        // Google Play by calling the startConnection() method.
        billingClient.startConnection(this)
      }
    })
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    Log.d(TAG, "onAttachedToActivity");
    this.activity = binding.activity;
  }

  override fun onDetachedFromActivityForConfigChanges() {

  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {

  }

  override fun onDetachedFromActivity() {

  }

  //支付完成的通知
  override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
      for (purchase in purchases) {
        handlePurchase(purchase)
      }
    } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
      // Handle an error caused by a user cancelling the purchase flow.
    } else {
      // Handle any other error codes.
    }
  }

  private fun handlePurchase(purchase: Purchase){
    //是消耗型商品
    if(isConsumedProduct) {
      billingClient.consumeAsync(ConsumeParams.newBuilder()
//              .setDeveloperPayload(purchase.developerPayload)
              .setPurchaseToken(purchase.purchaseToken).build())
      { billingResult, purchaseToken ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
          val purchaseMap = mapOf("purchase" to purchase.originalJson, "token" to purchaseToken)
          _result.success(purchaseMap)
        } else {
          //消费失败，后面查询消费记录再次付款，否则，只能等待退款
          _result.error("-1", "fail to acknowledge consumed product transaction", "")
        }
        finishTransaction()
      }
    } else {
      //是订阅等非消耗型商品
      if (purchase.purchaseState === Purchase.PurchaseState.PURCHASED) {
        // Acknowledge the purchase if it hasn't already been acknowledged.
        if (!purchase.isAcknowledged) {
          val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
//                                          .setDeveloperPayload(purchase.developerPayload)
                                          .setPurchaseToken(purchase.purchaseToken)
          billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
          { billingResult ->
            if(billingResult.responseCode == BillingClient.BillingResponseCode.OK){
              val purchaseMap = mapOf("purchase" to purchase.originalJson)
              _result.success(purchaseMap)
            }else{
              //订阅失败，后面查询消费记录再次付款，否则，只能等待退款
              _result.error("-1", "fail to acknowledge subscription order", "")
            }
            finishTransaction()
          }
        }
      }
    }
  }

  private fun querySkuDetails(purchaseId: String?, isProduct: Boolean) {
    val skuList: MutableList<String?> = ArrayList()
    skuList.add(purchaseId)
    val params = SkuDetailsParams.newBuilder()
    if(isProduct){
      params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
    }else{
      params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
    }
    billingClient.querySkuDetailsAsync(params.build(),
            SkuDetailsResponseListener { billingResult, skuDetailsList ->
              Log.e(TAG, "onSkuDetailsResponse code = " + billingResult.responseCode.toString() + " ,  msg = " + billingResult.debugMessage.toString() + " , skuDetailsList = " + skuDetailsList)
              // Process the result.
              Log.e(TAG, "products info is  $skuDetailsList")
              if (skuDetailsList == null || skuDetailsList.isEmpty()) {
                return@SkuDetailsResponseListener
              }
              var skuDetails: SkuDetails? = null
              for (details in skuDetailsList) {
                Log.e(TAG, "onSkuDetailsResponse skuDetails = $details")
                if (purchaseId == details.sku) {
                  skuDetails = details
                  break
                }
              }
              if (skuDetails != null) {
                affordService(skuDetails)
              }
            }
    )
  }

  private fun affordService(skuDetails: SkuDetails): Int{
    val flowParams = BillingFlowParams.newBuilder()
                                      .setSkuDetails(skuDetails)
                                      .build()
    val res =  billingClient.launchBillingFlow(activity, flowParams).responseCode
    Log.e(TAG, "AFFORD_SERVICE_RESULT_CODE: $res")
    return res;
  }

  //验证续订的有效性
  private fun verifySubscription(purchaseId: String?) {
    val purchaseResult: Purchase.PurchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
    val purchasesList: MutableList<Purchase>? = purchaseResult.purchasesList
    var purchaseRecord: Purchase? = null
    if (purchaseResult.responseCode == BillingClient.BillingResponseCode.OK
        && purchasesList != null) {
      for (purchase in purchasesList) {
        if (purchaseId == purchase.sku) {
          purchaseRecord = purchase
          break
        }
      }
    }
    if(purchaseRecord == null){
      _result.success(mapOf("purchase" to ""))
    }else{
      val purchaseMap = mapOf("purchase" to purchaseRecord.originalJson)
      _result.success(purchaseMap)
    }
    finishTransaction()
//    billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.SUBS,
//            PurchaseHistoryResponseListener { billingResult, purchasesList ->
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK
//                        && purchasesList != null) {
//                  var purchaseRecord: PurchaseHistoryRecord? = null
//                  for (purchase in purchasesList) {
//                    if(purchaseId == purchase.sku){
//                      purchaseRecord = purchase
//                      break
//                    }
//                  }
//                  val purchaseMap = mapOf("purchase" to purchaseRecord.toString())
//                  _result.success(purchaseMap)
//                }
//            }
//    )
  }

  //结束交易，断开谷歌支付链接
  private fun finishTransaction() {
    if(billingClient.isReady){
      billingClient.endConnection()
    }
  }

  //谷歌支付在连状态检查
  private fun isConnected(): Boolean {
    return billingClient != null && billingClient.isReady
  }

}
