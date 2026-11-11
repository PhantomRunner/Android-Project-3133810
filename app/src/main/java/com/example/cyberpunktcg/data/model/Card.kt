package com.example.cyberpunktcg.data.model

data class Card(
    override val id: String,
    override val name: String,
    override val type: CardType,
    override val color: CardColor,
    override val cost: Int?,
    override val power: Int?,
    override val ram: Int?,
    val subtitle: String,
    val unitTypes: List<String>,
    val description: String,
    val powerLabel: String? = null,
    val eddies: Boolean? = null,
    val imagePath: String? = null,
) : BrowsableCard
