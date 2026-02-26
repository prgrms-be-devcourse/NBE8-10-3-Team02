package com.back.domain.tag.tag.service;

import com.back.domain.tag.tag.entity.Tag;
import com.back.domain.tag.tag.repository.TagRepository;
import com.back.global.exception.ServiceException;
import com.back.global.igdb.IgdbDefensiveClient;
import com.back.global.igdb.dto.IgdbGameNameDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final IgdbDefensiveClient igdbClient;

    @Transactional
    public Tag create(String content) {
        tagRepository.findBycontent(content)
                .ifPresent(t -> {
                    throw new ServiceException("400-1","이미 존재하는 태그입니다.");
                });

        return tagRepository.save(new Tag(content));
    }

    public List<Tag> findAll(){
        return tagRepository.findAll();
    }

    public Optional<Tag> findById(int id) {
        return tagRepository.findById(id);
    }

    public void delete(Tag content) {
        tagRepository.delete(content);
    }

//    public void modify(Tag tag, String content) {
//        tag.modify(content);
//    }

    public Tag getOrCreate(String content) {
        return tagRepository.findBycontent(content)
                .orElseGet(() -> tagRepository.save(new Tag(content)));
    }

    //igdb에서 게임 제목을 태그로 가져옴
    @Transactional
    public List<Tag> createTagsFromIgdb(long igdbId) {
        /*TODO: game name만 검색하는거 따로 빼기*/
        IgdbGameNameDto game = igdbClient.getGameName(igdbId);

        if (game == null) {
            throw new ServiceException("404-3", "IGDB에서 정보를 찾을 수 없습니다.");
        }

        List<Tag> tags = new ArrayList<>();
        tags.add(getOrCreate(game.getName()));


        return tags;
    }
}
