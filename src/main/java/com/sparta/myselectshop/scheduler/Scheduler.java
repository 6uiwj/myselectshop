package com.sparta.myselectshop.scheduler;

import com.sparta.myselectshop.entity.Product;
import com.sparta.myselectshop.naver.dto.ItemDto;
import com.sparta.myselectshop.naver.service.NaverApiService;
import com.sparta.myselectshop.repository.ProductRepository;
import com.sparta.myselectshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "Scheduler")
@Component
@RequiredArgsConstructor
public class Scheduler {

    private final NaverApiService naverApiService; //아이템 List 목록에서 재검색해야 함
    private final ProductService productService; //검색해야할 프로덕트 목록 가져와야 함
    private final ProductRepository productRepository;

    //@Scheduled 지정한 특정 시간마다 메서드 동작
    // 초, 분, 시, 일, 월, 주 순서
    @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시
    public void updatePrice() throws InterruptedException {
        log.info("가격 업데이트 실행");
        List<Product> productList = productRepository.findAll();
        for (Product product : productList) {
            // 1초에 한 상품 씩 조회합니다 (NAVER 제한)
            TimeUnit.SECONDS.sleep(1);

            // i 번째 관심 상품의 제목으로 검색을 실행합니다.
            String title = product.getTitle();
            List<ItemDto> itemDtoList = naverApiService.searchItems(title); //naver에서 검색

            if (itemDtoList.size() > 0) { //검색된 결과가 있다면
                ItemDto itemDto = itemDtoList.get(0); //가장 상단 아이템을 가져옴
                // i 번째 관심 상품 정보를 업데이트합니다.
                Long id = product.getId(); //아이템의 id
                try {
                    productService.updateBySearch(id, itemDto); //update할 정보가 담긴 dto를 넘겨줌
                } catch (Exception e) {
                    log.error(id + " : " + e.getMessage());
                }
            }
        }
    }

}