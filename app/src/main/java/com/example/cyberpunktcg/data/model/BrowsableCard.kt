package com.example.cyberpunktcg.data.model

// Search and sorting use these shared fields for both catalog records and deck cards.
interface BrowsableCard {
    val id: String
    val name: String
    val type: CardType
    val color: CardColor
    val cost: Int?
    val power: Int?
    val ram: Int?
}
