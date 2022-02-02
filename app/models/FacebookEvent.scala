package models

import play.api.libs.json.{ JsObject, JsValue }

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

case class FacebookEvent(
    val name: FacebookEventName.Val,
    val properties: Option[JsObject] = None)