package models

import scala.language.implicitConversions

import play.api.libs.json.{ JsArray, JsBoolean, JsNumber, Json, JsString, JsValue }
import squants.market

object FacebookEventName extends Enumeration {
    case class FacebookEventNameValue(val name: String) extends super.Val {
        override def toString = name
    }

    implicit def valueToFacebookEventNameValue(v: Value): FacebookEventNameValue =
        v.asInstanceOf[FacebookEventNameValue]

    val AddPaymentInfo = FacebookEventNameValue("AddPaymentInfo")
    val AddToCart = FacebookEventNameValue("AddToCart")
    val AddToWishlist = FacebookEventNameValue("AddToWishlist")
    val CompleteRegistration = FacebookEventNameValue("CompleteRegistration")
    val Contact = FacebookEventNameValue("Contact")
    val CustomizeProduct = FacebookEventNameValue("CustomizeProduct")
    val Donate = FacebookEventNameValue("Donate")
    val FindLocation = FacebookEventNameValue("FindLocation")
    val InitiateCheckout = FacebookEventNameValue("InitiateCheckout")
    val Lead = FacebookEventNameValue("Lead")
    val PageView = FacebookEventNameValue("PageView")
    val Purchase = FacebookEventNameValue("Purchase")
    val Schedule = FacebookEventNameValue("Schedule")
    val Search = FacebookEventNameValue("Search")
    val SubmitApplication = FacebookEventNameValue("SubmitApplication")
    val Subscribe = FacebookEventNameValue("Subscribe")
    val ViewContent = FacebookEventNameValue("ViewContent")
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
    case class FacebookContentTypeValuesValue(val name: String) extends super.Val

    implicit def valueToFacebookContentTypeValuesValue(v: Value):
        FacebookContentTypeValuesValue = v.asInstanceOf[FacebookContentTypeValuesValue]

    val Product = FacebookContentTypeValuesValue("product")
    val ProductGroup = FacebookContentTypeValuesValue("product_group")
}

final case class FacebookContentType(
    val value: FacebookContentTypeValues.FacebookContentTypeValuesValue)
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
    case class FacebookDeliveryCategoryValueValue(val name: String) extends super.Val

    implicit def valueToFacebookDeliveryCategoryValueValue(v: Value):
        FacebookDeliveryCategoryValueValue = v.asInstanceOf[FacebookDeliveryCategoryValueValue]

    val InStore = FacebookDeliveryCategoryValueValue("in_store")
    val Curbside = FacebookDeliveryCategoryValueValue("curbside")
    val HomeDelivery = FacebookDeliveryCategoryValueValue("home_delivery")
}

final case class FacebookDeliveryCategory(
    val value: FacebookDeliveryCategoryValue.FacebookDeliveryCategoryValueValue)
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
    val name: FacebookEventName.FacebookEventNameValue,
    val properties: Seq[FacebookEventProperty] = Seq.empty)