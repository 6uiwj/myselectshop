package com.sparta.myselectshop.service;

import com.sparta.myselectshop.dto.FolderResponseDto;
import com.sparta.myselectshop.entity.Folder;
import com.sparta.myselectshop.entity.User;
import com.sparta.myselectshop.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;

    //폴더 생성하기
    public void addFolders(List<String> folderNames, User user) {
        //폴더의 이름을 기준으로 회원이 이미 생성한 폴더를 조회
        List<Folder> existFolderList = folderRepository.findAllByUserAndNameIn(user, folderNames);

        List<Folder> folderList = new ArrayList<>();

        //들어온 폴더 이름과, DB에서찾아온 폴더 이름을 비교해서 중복이 있는지 확인(이미 만든 폴더인지)
        for (String folderName : folderNames) {
            //중복되지 않으면 폴더 생성
            if(!isExistFolderName(folderName, existFolderList)) {
                Folder folder = new Folder(folderName, user);
                folderList.add(folder);

            } else { //중복된 폴더가 존재
                throw new IllegalArgumentException("폴더명이 중복되었습니다.");
            }
        }
            folderRepository.saveAll(folderList);

    }
    //폴더 전부 가져오기
    public List<FolderResponseDto> getFolders(User user) {
        List<Folder> forderList = folderRepository.findAllByUser(user);
        List<FolderResponseDto> responseDtoList = new ArrayList<>();

        //folder를 FolderResponseDto에 담아서 List에 넣기
        for (Folder folder : forderList) {
            responseDtoList.add(new FolderResponseDto(folder));
        }
        return responseDtoList;
    }

    private boolean isExistFolderName(String folderName, List<Folder> existFolderList) {
        for (Folder existFolder : existFolderList) {
            if(folderName.equals(existFolder.getName())) {
                return true;
            }
        }
        return false;
    }
}
