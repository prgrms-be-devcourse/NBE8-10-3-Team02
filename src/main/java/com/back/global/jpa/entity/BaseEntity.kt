package com.back.global.jpa.entity

import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.Objects

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
        protected set // 외부에서는 수정 불가, 하위 클래스와 패키지 내에서는 수정 가능

    // 자바의 setId(id)와 동일한 역할을 하기 위해 public open으로 메서드를 열어둘 수도 있지만,
    // 코틀린에서는 프로퍼티에 직접 접근하는 것이 일반적입니다.

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BaseEntity) return false
        if (id == 0 || other.id == 0) return false
        return id == other.id
    }

    override fun hashCode(): Int = Objects.hashCode(id)
}
