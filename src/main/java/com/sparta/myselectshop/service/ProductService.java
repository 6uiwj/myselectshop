package com.sparta.myselectshop.service;

import com.sparta.myselectshop.dto.ProductMypriceRequestDto;
import com.sparta.myselectshop.dto.ProductRequestDto;
import com.sparta.myselectshop.dto.ProductResponseDto;
import com.sparta.myselectshop.entity.*;
import com.sparta.myselectshop.naver.dto.ItemDto;
import com.sparta.myselectshop.repository.FolderRepository;
import com.sparta.myselectshop.repository.ProductFolderRepository;
import com.sparta.myselectshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    private final FolderRepository folderRepository;

    public static final int MIN_MY_PRICE = 100; //myprice 최저가 100원으로 설정
    private final ProductFolderRepository productFolderRepository;

    public ProductResponseDto createProduct(ProductRequestDto requestDto, User user) {
        Product product = productRepository.save(new Product(requestDto, user));
        return new ProductResponseDto(product);
    }

    @Transactional //변경 감지를 위해 DirtChecking이 되도록 트랜잭션 걸어주기
    public ProductResponseDto updateProduct(Long id, ProductMypriceRequestDto requestDto) {
        //가격 가져오기
        int myprice = requestDto.getMyprice();
        if(myprice < MIN_MY_PRICE) { //내 설정가가 최저가보다 작으면 exception
            throw new IllegalArgumentException("유효햐지 않는 관심 가격입니다. 최소 " + MIN_MY_PRICE+"원 이상으로 설정해주세요.");
        }

        //해당 물건이 있는가 -> 없으면 exception
        Product product = productRepository.findById(id).orElseThrow(() ->
                new NullPointerException("해당 상품을 찾을 수 없습니다.")
                );

        product.update(requestDto);

        return new ProductResponseDto(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(User user, int page, int size, String sortBy, boolean isAsc) {
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        //권한 확인 후 모든 제품 조회기능을 줄지 안줄지
        UserRoleEnum userRoleEnum = user.getRole();

        Page<Product> productList;

        //일반 유저이면 해당 유저가 관심등록한 상품만 조회
        if (userRoleEnum == UserRoleEnum.USER) {
            productList = productRepository.findAllByUser(user, pageable);
        } else { //관리자면 모든 상품 조회
            productList = productRepository.findAll(pageable);

        }
        //page타입에 있는 product data를 하나씩 꺼내와 productResponseDto 생성자가 하나씩 호출,
        // -> product를 productResponseDto로 변환
        return productList.map(ProductResponseDto::new);
    }

    @Transactional //dirty checking 을 위해
    public void updateBySearch(Long id, ItemDto itemDto) {
        Product product = productRepository.findById(id).orElseThrow(() ->
                new NullPointerException("해당 상품은 존재하지 않습니다.")
        );
        product.updateByItemDto(itemDto);
    }

    public void addFolder(Long productId, Long folderId, User user) {
        Product product = productRepository.findById(productId).orElseThrow(
                () -> new NullPointerException("해당 상품이 존재하지 않습니다.")
        );

        Folder folder = folderRepository.findById(folderId).orElseThrow(
                () -> new NullPointerException("해당 폴더가 존재하지 않습니다.")
        );


        //현재로그인한 유저가 등록한 폴더와 상품이 맞는지 확인
        //현재 로그인한 유저와 이 상품을 등록한 유저가 다르거나, 현재로그인한 유저와 이 폴더를 만든 유저가 다르다면
        if( !product.getUser().getId().equals(user.getId()) || !folder.getUser().getId().equals(user.getId()) ) {
            throw new IllegalArgumentException("회원님의 관심상품이 아니거나, 회원님의 폴더가 아닙니다.");
        }

        //하나의 상품이 이미 등록된 폴더에 다시 등록될 수없다. (이미 존재하는 폴더인지 중복확인)
        Optional<ProductFolder> overlapFolder = productFolderRepository.findByProductAndFolder(product, folder);

        if(overlapFolder.isPresent()) {
            throw new IllegalArgumentException("중복된 폴더입니다.");
        }

        //productFolder에 등록 (ProductFolder가 외래키의 주인이므로 외래키도 넣어주야함)
        productFolderRepository.save(new ProductFolder(product, folder));
    }

//    public List<ProductResponseDto> getAllProducts() {
//        List<Product> productList = productRepository.findAll();
//        List<ProductResponseDto> responseDtoList = new ArrayList<>(); //반환값 넣어줄곳
//
//        //product객체를 productResponseDto 객체로 변환
//        for (Product product : productList) {
//            responseDtoList.add(new ProductResponseDto(product));
//        }
//
//        return  responseDtoList;
//    }
}
