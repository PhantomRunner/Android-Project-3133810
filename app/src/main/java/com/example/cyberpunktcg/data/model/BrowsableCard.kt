package com.example.cyberpunktcg.data.model

interface BrowsableCard {
    val id: String
    val name: String
    val type: CardType
    val color: CardColor
    val cost: Int?
    val power: Int?
    val ram: Int?
}
