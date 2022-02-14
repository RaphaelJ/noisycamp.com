package models

import play.api.libs.json.{ JsArray, JsBoolean, JsNumber, Json, JsString, JsValue }
import squants.market

object FacebookEventName extends Enumeration {
    case class Val(val name: String)

    val AddPaymentInfo = Val("AddPaymentInfo")
    val AddToCart = Val("AddToCart")
    val AddToWishlist = Val("AddToWishlist")
    val CompleteRegistration = Val("CompleteRegistration")
    val Contact = Val("Contact")
    val CustomizeProduct = Val("CustomizeProduct")
    val Donate = Val("Donate")
    val FindLocation = Val("FindLocation")
    val InitiateCheckout = Val("InitiateCheckout")
    val Lead = Val("Lead")
    val PageView = Val("PageView")
    val Purchase = Val("Purchase")
    val Schedule = Val("Schedule")
    val Search = Val("Search")
    val SubmitApplication = Val("SubmitApplication")
    val Subscribe = Val("Subscribe")
    val ViewContent = Val("ViewContent")
}

sealed trait FacebookEventProperty {
    /** Returns the property's value as a (key, value) pair. */
    def asJson: (String, JsValue)
}

final case class FacebookContentCategory(val value: String) extends FacebookEventProperty {
    def asJson = "content_category" -> JsString(value)
}

final case class FacebookContentIds(val values: Seq[String]) extends FacebookEventProperty {
    def asJson = "content_ids" -> JsArray(values.map(JsString(_)))
}

final case class FacebookContentName(val value: String) extends FacebookEventProperty {
    def asJson = "content_name" -> JsString(value)
}

object FacebookContentTypeValues extends Enumeration {
    case class Val(val name: String)

    val Product = Val("product")
    val ProductGroup = Val("product_group")
}

final case class FacebookContentType(val value: FacebookContentTypeValues.Val)
    extends FacebookEventProperty {

    def asJson = "content_type" -> JsString(value.name)
}

case class FacebookContentValue(
    val id: String,
    val quantity: Int,
)

final case class FacebookContent(val values: Seq[FacebookContentValue])
    extends FacebookEventProperty {

    def asJson = {
        val jsonValues = values.map { v => Json.obj("id" -> v.id, "quantity" -> v.quantity) }
        "contents" -> JsArray(jsonValues)
    }
}

final case class FacebookCurrency(val value: market.Currency)
    extends FacebookEventProperty {

    def asJson = "currency" -> JsString(value.code)
}

object FacebookDeliveryCategoryValue extends Enumeration {
    case class Val(val name: String)

    val InStore = Val("in_store")
    val Curbside = Val("curbside")
    val HomeDelivery = Val("home_delivery")
}

final case class FacebookDeliveryCategory(val value: FacebookDeliveryCategoryValue.Val)
    extends FacebookEventProperty {

    def asJson = "delivery_category" -> JsString(value.name)
}

final case class FacebookNumItems(val value: Int)
    extends FacebookEventProperty {

    def asJson = "num_items" -> JsNumber(value)
}

final case class FacebookPredictedLtv(val value: BigDecimal) extends FacebookEventProperty {

    def asJson = "predicted_ltv" -> JsNumber(value)
}

final case class FacebookSearchString(val value: String) extends FacebookEventProperty {

    def asJson = "search_string" -> JsString(value)
}

final case class FacebookStatus(val value: Boolean) extends FacebookEventProperty {

    def asJson = "status" -> JsBoolean(value)
}

final case class FacebookValue(val value: BigDecimal) extends FacebookEventProperty {

    def asJson = "value" -> JsNumber(value)
}


case class FacebookEvent(
    val name: FacebookEventName.Val,
    val properties: Seq[FacebookEventProperty] = Seq.empty)