package com.app.recychool.service;

import com.app.recychool.domain.dto.PaymentCompleteRequestDTO;
import com.app.recychool.domain.dto.PaymentCompleteResponseDTO;
import com.app.recychool.domain.dto.PaymentPageResponseDTO;
import com.app.recychool.domain.entity.Payment;
import com.app.recychool.domain.entity.Reserve;
import com.app.recychool.domain.enums.ReserveStatus;
import com.app.recychool.domain.enums.ReserveType;
import com.app.recychool.domain.type.PaymentStatus;
import com.app.recychool.exception.PaymentAlreadyProcessedException;
import com.app.recychool.repository.PaymentRepository;
import com.app.recychool.repository.ReserveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReserveRepository reserveRepository;

    @Override
    public PaymentCompleteResponseDTO completePayment(PaymentCompleteRequestDTO requestDTO) {

        // 1) 예약 조회
        Reserve reserve = reserveRepository.findById(requestDTO.getReserveId())
                .orElseThrow(() ->
                        new IllegalArgumentException("예약이 존재하지 않습니다. reserveId=" + requestDTO.getReserveId())
                );

        // 2) 서버 기준 결제 가능 상태 검증
        if (!requestDTO.isExtend()) {
            if (reserve.getReserveStatus() != ReserveStatus.PENDING) {
                throw new PaymentAlreadyProcessedException(
                        "결제 가능한 상태가 아닙니다. reserveStatus=" + reserve.getReserveStatus()
                );
            }
        } else {
            if (reserve.getReserveType() != ReserveType.PARKING) {
                throw new PaymentAlreadyProcessedException("연장 결제는 주차 예약만 가능합니다.");
            }
            if (reserve.getReserveStatus() != ReserveStatus.COMPLETED) {
                throw new PaymentAlreadyProcessedException(
                        "연장 가능한 상태가 아닙니다. reserveStatus=" + reserve.getReserveStatus()
                );
            }
        }

        // 3) 결제 중복 정책
        // 장소 대여: 예약 1건당 결제 1건만 허용
        if (reserve.getReserveType() == ReserveType.PLACE) {
            if (paymentRepository.existsByReserve_Id(reserve.getId())) {
                throw new PaymentAlreadyProcessedException("이미 결제가 완료된 장소 대여 예약입니다.");
            }
        }

        // 주차 예약: 연장 결제(isExtend=true)가 아닌 경우만 중복 차단
        if (reserve.getReserveType() == ReserveType.PARKING && !requestDTO.isExtend()) {
            if (paymentRepository.existsByReserve_Id(reserve.getId())) {
                throw new PaymentAlreadyProcessedException("이미 결제가 완료된 주차 예약입니다.");
            }
        }

        // 4) impUid 중복 차단
        if (paymentRepository.existsByImpUid(requestDTO.getImpUid())) {
            throw new PaymentAlreadyProcessedException(
                    "이미 처리된 결제입니다. impUid=" + requestDTO.getImpUid()
            );
        }

        // 5) 결제 저장
        Payment payment = Payment.builder()
                .reserve(reserve)
                .paymentPrice(reserve.getReservePrice())
                .impUid(requestDTO.getImpUid())
                .merchantUid(requestDTO.getMerchantUid())
                .paymentStatus(PaymentStatus.PAID)
                .paymentType(requestDTO.getPaymentType())
                .build();

        Payment saved = paymentRepository.save(payment);

        // 6) 예약 상태 업데이트
        if (!requestDTO.isExtend()) {
            reserve.setReserveStatus(ReserveStatus.COMPLETED);
        }

        return new PaymentCompleteResponseDTO(
                saved.getId(),
                reserve.getId(),
                saved.getPaymentStatus(),
                saved.getPaymentPrice()
        );
    }


    @Override
    public PaymentPageResponseDTO getReserve(Long reserveId, boolean extend) {

        Reserve reserve = reserveRepository.findById(reserveId)
                .orElseThrow(() -> new IllegalArgumentException("예약이 존재하지 않습니다."));

        LocalDate start = reserve.getStartDate();
        LocalDate end = reserve.getEndDate();

        //  PARKING + extend=true면 (기존 종료일+1) ~ (그로부터 1개월)
        if (extend && reserve.getReserveType() == ReserveType.PARKING) {
            start = end.plusDays(1);
            end = start.plusMonths(1);
        }

        return new PaymentPageResponseDTO(
                reserve.getId(),
                reserve.getReserveType().name(),
                start.toString(),
                end.toString(),
                reserve.getReservePrice(),
                reserve.getSchool().getId(),
                reserve.getUser().getUserName(),
                reserve.getUser().getUserEmail(),
                reserve.getUser().getUserPhone(),
                reserve.getSchool().getSchoolName(),
                reserve.getSchool().getSchoolAddress()
        );
    }
}
