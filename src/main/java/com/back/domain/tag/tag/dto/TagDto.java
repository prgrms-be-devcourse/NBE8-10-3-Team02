package com.back.domain.tag.tag.dto;

import com.back.domain.tag.tag.entity.Tag;

public record TagDto (
        int id,
    String content
) {

    public TagDto(Tag tag) {
        this(
                tag.getId(),
                tag.getContent()
        );
    }
}