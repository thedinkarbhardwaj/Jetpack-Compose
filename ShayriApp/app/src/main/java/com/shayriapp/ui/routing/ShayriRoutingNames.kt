package com.shayriapp.ui.routing



sealed class ShayriRoutingNames(val route:String) {

    object splashScreen:ShayriRoutingNames("splash")
    object categoryScreen:ShayriRoutingNames("category")
    object shayariScreen:ShayriRoutingNames("shayari")
}