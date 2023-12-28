package com.github.goomon.jpa.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "post")
data class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "title")
    val title: String,

    @Column(name = "creator")
    val creator: String,

    @Column(name = "created_at", insertable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
