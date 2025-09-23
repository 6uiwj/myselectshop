package com.sparta.myselectshop.service;

import com.sparta.myselectshop.dto.ProductMypriceRequestDto;
import com.sparta.myselectshop.dto.ProductRequestDto;
import com.sparta.myselectshop.dto.ProductResponseDto;
import com.sparta.myselectshop.entity.Product;
import com.sparta.myselectshop.naver.dto.ItemDto;
import com.sparta.myselectshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public static final int MIN_MY_PRICE = 100; //myprice 최저가 100원으로 설정

    public ProductResponseDto createProduct(ProductRequestDto requestDto) {
        Product product = productRepository.save(new Product(requestDto));
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

    public List<ProductResponseDto> getProducts() {
        List<Product> productList = productRepository.findAll();
        List<ProductResponseDto> responseDtoList = new ArrayList<>(); //반환값 넣어줄곳

        //product객체를 productResponseDto 객체로 변환
        for (Product product : productList) {
            responseDtoList.add(new ProductResponseDto(product));
        }

        return  responseDtoList;
    }

    @Transactional //dirty checking 을 위해
    public void updateBySearch(Long id, ItemDto itemDto) {
        Product product = productRepository.findById(id).orElseThrow(() ->
                new NullPointerException("해당 상품은 존재하지 않습니다.")
        );
        product.updateByItemDto(itemDto);
    }
}
