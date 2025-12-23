package com.app.recychool.service;

import com.app.recychool.domain.entity.Reserve;
import com.app.recychool.repository.ReserveRepository;
import com.app.recychool.service.ReserveExpireService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReserveExpireServiceImpl implements ReserveExpireService {

    private final ReserveRepository reserveRepository;

    @Override
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional
    public void expireReserves() {
        LocalDate today = LocalDate.now();

        List<Reserve> targets =
                reserveRepository.findCompletedAndExpired(today);

        for (Reserve reserve : targets) {
            reserve.expire(today);
        }
    }
}
