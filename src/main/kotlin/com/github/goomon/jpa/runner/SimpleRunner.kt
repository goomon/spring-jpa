package com.github.goomon.jpa.runner

import com.github.goomon.jpa.domain.Post
import jakarta.persistence.EntityManager
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class SimpleRunner(
    private val em: EntityManager,
) {
    @EventListener
    fun onApplicationEvent(event: ApplicationEvent) {
        if (event is ApplicationReadyEvent) {
            main()
        }
    }

    fun main() {
        val post = Post(
            title = "test",
            creator = "Sam"
        )
        em.persist(post)
    }
}