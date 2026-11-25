package com.mzlhsc.orpl.order.service;

import com.mzlhsc.orpl.order.domain.dto.DiningAaPayDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningCartDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningCheckoutDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningJoinDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningMergeDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningOpenTableDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningReservationDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningTableDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningTransferDTO;
import com.mzlhsc.orpl.order.domain.po.DiningReservation;
import com.mzlhsc.orpl.order.domain.po.DiningTable;
import com.mzlhsc.orpl.order.domain.po.ShoppingCart;
import com.mzlhsc.orpl.order.domain.vo.DiningReportVO;
import com.mzlhsc.orpl.order.domain.vo.DiningSessionVO;
import com.mzlhsc.orpl.order.domain.vo.DiningTableVO;
import com.mzlhsc.orpl.order.domain.vo.OrderSubmitVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IDiningService {

    // ==================== 桌台 ====================

    List<DiningTableVO> listTables(Integer status, String area);

    DiningTable createTable(DiningTableDTO dto);

    DiningTable updateTable(Long id, DiningTableDTO dto);

    void deleteTable(Long id);

    DiningTableVO getTableByQrToken(String qrToken);

    DiningTableVO refreshQrToken(Long tableId);

    // ==================== 预约 ====================

    DiningReservation createReservation(DiningReservationDTO dto);

    List<DiningReservation> listReservations(Integer status, LocalDate date);

    List<DiningReservation> myReservations();

    void confirmReservation(Long id);

    void cancelReservation(Long id);

    DiningReservation arriveReservation(Long id);

    // ==================== 会话 ====================

    List<DiningSessionVO> listSessions(Integer status, Long tableId);

    DiningSessionVO detail(Long sessionId);

    DiningSessionVO openTable(DiningOpenTableDTO dto);

    DiningSessionVO openTableByAdmin(DiningOpenTableDTO dto);

    DiningSessionVO joinSession(DiningJoinDTO dto);

    DiningSessionVO myActiveSession();

    Map<String, Object> scanTable(String qrToken);

    void enableAa(Long sessionId);

    DiningSessionVO requestCheckout(Long sessionId);

    void transferTable(Long sessionId, DiningTransferDTO dto);

    void mergeSessions(Long targetSessionId, DiningMergeDTO dto);

    void cancelSession(Long sessionId, String reason);

    Map<String, Object> aaPay(DiningAaPayDTO dto);

    void checkout(DiningCheckoutDTO dto);

    // ==================== 共享购物车 / 堂食下单 ====================

    List<ShoppingCart> listCart(Long sessionId);

    void addCartItem(DiningCartDTO dto);

    void removeCartItem(Long sessionId, Long cartId);

    void clearCart(Long sessionId);

    OrderSubmitVO submitDiningOrder(Long sessionId);

    // ==================== 报表 ====================

    DiningReportVO report(LocalDateTime begin, LocalDateTime end);
}
