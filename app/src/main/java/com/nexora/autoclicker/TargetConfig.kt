package com.nexora.autoclicker

data class TargetConfig(
    val id: Int,
    var x: Float,
    var y: Float,
    var startDelay: Long,
    var endDelay: Long,
    var duration: Long,
    var enabled: Boolean
)
