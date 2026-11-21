package com.example.data.local

enum class MediaType {
    IMAGE,
    VIDEO
}

enum class MediaStatus {
    ACTIVE,
    HIDDEN,
    TRASHED
}

enum class ViewDensity(val columns: Int, val label: String) {
    COMPACT(4, "Miniaturas"),
    STANDARD(3, "Cuadrícula"),
    COZY(2, "Amplia")
}

enum class MediaFilter(val label: String) {
    ALL("Todos"),
    IMAGES("Fotos"),
    VIDEOS("Videos")
}
