package com.example.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uriString: String,
    val albumArtUriString: String?,
    val folderName: String,
    val dateAdded: Long
)
