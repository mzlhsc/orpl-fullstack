package com.mzlhsc.orpl.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mzlhsc.orpl.dish.domain.po.Dish;
import com.mzlhsc.orpl.user.domain.po.User;
import com.mzlhsc.orpl.dish.service.IDishService;
import com.mzlhsc.orpl.user.service.IUserService;
import com.mzlhsc.orpl.common.constant.MessageConstant;
import com.mzlhsc.orpl.common.context.UserContext;
import com.mzlhsc.orpl.common.exception.BadRequestException;
import com.mzlhsc.orpl.common.exception.BizIllegalException;
import com.mzlhsc.orpl.common.exception.ForbiddenException;
import com.mzlhsc.orpl.common.exception.NotFoundException;
import com.mzlhsc.orpl.order.domain.dto.DiningAaPayDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningCartDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningCheckoutDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningJoinDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningMergeDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningOpenTableDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningReservationDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningTableDTO;
import com.mzlhsc.orpl.order.domain.dto.DiningTransferDTO;
import com.mzlhsc.orpl.order.domain.po.DiningPayment;
import com.mzlhsc.orpl.order.domain.po.DiningReservation;
import com.mzlhsc.orpl.order.domain.po.DiningSession;
import com.mzlhsc.orpl.order.domain.po.DiningSessionMember;
import com.mzlhsc.orpl.order.domain.po.DiningSessionTable;
import com.mzlhsc.orpl.order.domain.po.DiningTable;
import com.mzlhsc.orpl.order.domain.po.DiningTableOperation;
import com.mzlhsc.orpl.order.domain.po.OrderDetail;
import com.mzlhsc.orpl.order.domain.po.Orders;
import com.mzlhsc.orpl.order.domain.po.ShoppingCart;
import com.mzlhsc.orpl.order.domain.vo.DiningReportVO;
import com.mzlhsc.orpl.order.domain.vo.DiningOrderRoundVO;
import com.mzlhsc.orpl.order.domain.vo.DiningSessionVO;
import com.mzlhsc.orpl.order.domain.vo.DiningTableVO;
import com.mzlhsc.orpl.order.domain.vo.OrderSubmitVO;
import com.mzlhsc.orpl.order.mapper.DiningPaymentMapper;
import com.mzlhsc.orpl.order.mapper.DiningReservationMapper;
import com.mzlhsc.orpl.order.mapper.DiningSessionMapper;
import com.mzlhsc.orpl.order.mapper.DiningSessionMemberMapper;
import com.mzlhsc.orpl.order.mapper.DiningSessionTableMapper;
import com.mzlhsc.orpl.order.mapper.DiningTableMapper;
import com.mzlhsc.orpl.order.mapper.DiningTableOperationMapper;
import com.mzlhsc.orpl.order.mapper.OrderDetailMapper;
import com.mzlhsc.orpl.order.mapper.OrdersMapper;
import com.mzlhsc.orpl.order.service.DishStockManager;
import com.mzlhsc.orpl.order.service.IDiningService;
import com.mzlhsc.orpl.order.service.IShoppingCartService;
import com.mzlhsc.orpl.order.service.OrderRateLimiter;
import com.mzlhsc.orpl.shop.service.ShopSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 堂食完整场景：桌台、预约、开台/加入、共享购物车、加菜下单、转台/并桌、AA 与整单结账、报表。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DiningServiceImpl implements IDiningService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DiningTableMapper tableMapper;
    private final DiningSessionMapper sessionMapper;
    private final DiningSessionMemberMapper memberMapper;
    private final DiningSessionTableMapper sessionTableMapper;
    private final DiningReservationMapper reservationMapper;
    private final DiningPaymentMapper paymentMapper;
    private final DiningTableOperationMapper operationMapper;

    private final IShoppingCartService shoppingCartService;    private final OrdersMapper ordersMapper;
    private final OrderDetailMapper orderDetailMapper;

    private final IDishService dishService;
    private final IUserService userService;
    private final DishStockManager dishStockManager;
    private final OrderRateLimiter orderRateLimiter;
    private final ShopSettingsService shopSettingsService;

    // ==================== 桌台 ====================

    @Override
    public List<DiningTableVO> listTables(Integer status, String area) {
        List<DiningTable> tables = tableMapper.selectList(Wrappers.<DiningTable>lambdaQuery()
                .eq(status != null, DiningTable::getStatus, status)
                .eq(StringUtils.hasText(area), DiningTable::getArea, area)
                .orderByAsc(DiningTable::getName));
        return tables.stream().map(this::toTableVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningTable createTable(DiningTableDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) {
            throw new BadRequestException("桌台名称不能为空");
        }
        if (tableMapper.selectCount(Wrappers.<DiningTable>lambdaQuery()
                .eq(DiningTable::getName, dto.getName())) > 0) {
            throw new BizIllegalException("桌台名称已存在");
        }
        DiningTable table = DiningTable.builder()
                .name(dto.getName())
                .seats(dto.getSeats() == null ? 4 : dto.getSeats())
                .area(dto.getArea())
                .status(DiningTable.FREE)
                .qrToken(newQrToken())
                .reservationEnabled(dto.getReservationEnabled() == null ? 1 : dto.getReservationEnabled())
                .build();
        tableMapper.insert(table);
        return table;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningTable updateTable(Long id, DiningTableDTO dto) {
        DiningTable table = requireTable(id);
        if (dto == null) {
            throw new BadRequestException("参数不能为空");
        }
        if (StringUtils.hasText(dto.getName()) && !Objects.equals(dto.getName(), table.getName())) {
            if (tableMapper.selectCount(Wrappers.<DiningTable>lambdaQuery()
                    .eq(DiningTable::getName, dto.getName())) > 0) {
                throw new BizIllegalException("桌台名称已存在");
            }
            table.setName(dto.getName());
        }
        if (dto.getSeats() != null) {
            table.setSeats(dto.getSeats());
        }
        if (dto.getArea() != null) {
            table.setArea(dto.getArea());
        }
        if (dto.getReservationEnabled() != null) {
            table.setReservationEnabled(dto.getReservationEnabled());
        }
        tableMapper.updateById(table);
        return table;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long id) {
        DiningTable table = requireTable(id);
        if (table.getStatus() != DiningTable.FREE) {
            throw new BizIllegalException("桌台正在使用中，不能删除");
        }
        if (sessionTableMapper.selectCount(Wrappers.<DiningSessionTable>lambdaQuery()
                .eq(DiningSessionTable::getTableId, id)) > 0) {
            throw new BizIllegalException("桌台存在历史会话，建议停用而不是删除");
        }
        if (reservationMapper.selectCount(Wrappers.<DiningReservation>lambdaQuery()
                .eq(DiningReservation::getTableId, id)
                .in(DiningReservation::getStatus, 1, 2)) > 0) {
            throw new BizIllegalException("桌台存在未完成预约，不能删除");
        }
        tableMapper.deleteById(id);
    }

    @Override
    public DiningTableVO getTableByQrToken(String qrToken) {
        if (!StringUtils.hasText(qrToken)) {
            throw new BadRequestException("桌台二维码不能为空");
        }
        DiningTable table = tableMapper.selectOne(Wrappers.<DiningTable>lambdaQuery()
                .eq(DiningTable::getQrToken, qrToken));
        if (table == null) {
            throw new NotFoundException("二维码无效或桌台已删除");
        }
        return toTableVO(table);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningTableVO refreshQrToken(Long tableId) {
        DiningTable table = requireTable(tableId);
        table.setQrToken(newQrToken());
        tableMapper.updateById(table);
        return toTableVO(table);
    }

    // ==================== 预约 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningReservation createReservation(DiningReservationDTO dto) {
        Long userId = requireUser();
        if (dto == null || dto.getTableId() == null || dto.getReserveTime() == null) {
            throw new BadRequestException("预约信息不完整");
        }
        DiningTable table = requireTable(dto.getTableId());
        if (table.getReservationEnabled() == null || table.getReservationEnabled() != 1) {
            throw new BizIllegalException("该桌台暂不支持预约");
        }
        if (dto.getGuestCount() > table.getSeats()) {
            throw new BizIllegalException("预约人数超过桌台座位数（" + table.getSeats() + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        if (dto.getReserveTime().isBefore(now.plusMinutes(30))) {
            throw new BizIllegalException("预约时间需在 30 分钟之后");
        }
        if (dto.getReserveTime().isAfter(now.plusDays(7))) {
            throw new BizIllegalException("最多支持提前 7 天预约");
        }
        long conflict = reservationMapper.selectCount(Wrappers.<DiningReservation>lambdaQuery()
                .eq(DiningReservation::getTableId, dto.getTableId())
                .in(DiningReservation::getStatus, 1, 2)
                .ge(DiningReservation::getReserveTime, dto.getReserveTime().minusMinutes(90))
                .le(DiningReservation::getReserveTime, dto.getReserveTime().plusMinutes(90)));
        if (conflict > 0) {
            throw new BizIllegalException("该时段桌台已被预约，请选择其他时间");
        }
        DiningReservation reservation = DiningReservation.builder()
                .tableId(dto.getTableId())
                .userId(userId)
                .contactName(dto.getContactName())
                .contactPhone(dto.getContactPhone())
                .reserveTime(dto.getReserveTime())
                .guestCount(dto.getGuestCount())
                .status(1)
                .remark(dto.getRemark())
                .build();
        reservationMapper.insert(reservation);
        return reservation;
    }

    @Override
    public List<DiningReservation> listReservations(Integer status, LocalDate date) {
        expireReservations();
        return reservationMapper.selectList(Wrappers.<DiningReservation>lambdaQuery()
                .eq(status != null, DiningReservation::getStatus, status)
                .ge(date != null, DiningReservation::getReserveTime, date == null ? null : date.atStartOfDay())
                .lt(date != null, DiningReservation::getReserveTime, date == null ? null : date.plusDays(1).atStartOfDay())
                .orderByDesc(DiningReservation::getReserveTime));
    }

    @Override
    public List<DiningReservation> myReservations() {
        Long userId = requireUser();
        expireReservations();
        return reservationMapper.selectList(Wrappers.<DiningReservation>lambdaQuery()
                .eq(DiningReservation::getUserId, userId)
                .orderByDesc(DiningReservation::getReserveTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReservation(Long id) {
        DiningReservation reservation = requireReservation(id);
        if (reservation.getStatus() != 1) {
            throw new BizIllegalException("只有待确认预约可以确认");
        }
        reservation.setStatus(2);
        reservationMapper.updateById(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReservation(Long id) {
        DiningReservation reservation = requireReservation(id);
        if (!UserContext.isAdmin()) {
            Long userId = requireUser();
            if (!Objects.equals(reservation.getUserId(), userId)) {
                throw new ForbiddenException("无权取消他人预约");
            }
        }
        if (reservation.getStatus() == 3 || reservation.getStatus() == 4) {
            throw new BizIllegalException("预约已到店或已取消");
        }
        reservation.setStatus(4);
        reservationMapper.updateById(reservation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningReservation arriveReservation(Long id) {
        DiningReservation reservation = requireReservation(id);
        if (reservation.getStatus() != 1 && reservation.getStatus() != 2) {
            throw new BizIllegalException("预约状态不允许到店确认");
        }
        reservation.setStatus(3);
        reservationMapper.updateById(reservation);
        return reservation;
    }

    // ==================== 会话 ====================

    @Override
    public List<DiningSessionVO> listSessions(Integer status, Long tableId) {
        List<Long> sessionIds = null;
        if (tableId != null) {
            sessionIds = sessionTableMapper.selectList(Wrappers.<DiningSessionTable>lambdaQuery()
                            .eq(DiningSessionTable::getTableId, tableId))
                    .stream().map(DiningSessionTable::getSessionId).toList();
            if (sessionIds.isEmpty()) {
                return List.of();
            }
        }
        List<DiningSession> sessions = sessionMapper.selectList(Wrappers.<DiningSession>lambdaQuery()
                .eq(status != null, DiningSession::getStatus, status)
                .in(sessionIds != null, DiningSession::getId, sessionIds)
                .orderByDesc(DiningSession::getOpenTime));
        return sessions.stream().map(s -> detail(s.getId())).toList();
    }

    @Override
    public DiningSessionVO detail(Long sessionId) {
        DiningSession session = requireSession(sessionId);
        if (!UserContext.isAdmin() && UserContext.getCurrentId() != null) {
            requireMember(sessionId, UserContext.getCurrentId());
        }
        DiningSessionVO vo = new DiningSessionVO();
        BeanUtils.copyProperties(session, vo);
        List<DiningSessionTable> mappings = sessionTableMapper.selectList(Wrappers.<DiningSessionTable>lambdaQuery()
                .eq(DiningSessionTable::getSessionId, sessionId));
        List<String> tableNames = new ArrayList<>();
        for (DiningSessionTable mapping : mappings) {
            DiningTable table = tableMapper.selectById(mapping.getTableId());
            if (table != null) {
                tableNames.add(table.getName());
            }
        }
        vo.setTableNames(tableNames);
        List<DiningSessionMember> members = memberMapper.selectList(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getSessionId, sessionId)
                .orderByAsc(DiningSessionMember::getJoinTime));
        vo.setMembers(members);
        BigDecimal paid = members.stream()
                .map(m -> m.getPaidAmount() == null ? BigDecimal.ZERO : m.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setPaidAmount(paid);
        BigDecimal total = session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount();
        vo.setTotalAmount(total);
        vo.setUnpaidAmount(total.subtract(paid).max(BigDecimal.ZERO));
        int memberCount = members.size();
        vo.setMemberCount(memberCount);
        if (memberCount > 0) {
            vo.setPerPersonAmount(total.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP));
        } else {
            vo.setPerPersonAmount(total);
        }
        List<ShoppingCart> cart = shoppingCartService.lambdaQuery()
                .eq(ShoppingCart::getDiningSessionId, sessionId)
                .orderByAsc(ShoppingCart::getId)
                .list();
        vo.setCartItems(cart);
        vo.setCartAmount(cart.stream()
                .map(c -> (c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                        .multiply(BigDecimal.valueOf(c.getNumber() == null ? 0 : c.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getDiningSessionId, sessionId)
                .orderByAsc(Orders::getOrderTime)
                .orderByAsc(Orders::getId));
        vo.setOrders(orders);
        List<DiningOrderRoundVO> orderRounds = buildOrderRounds(orders);
        vo.setOrderRounds(orderRounds);
        if (!orderRounds.isEmpty()) {
            DiningOrderRoundVO currentRound = orderRounds.get(orderRounds.size() - 1);
            vo.setCurrentOrderId(currentRound.getOrderId());
            vo.setCurrentOrderNo(currentRound.getOrderNumber());
        } else if (!orders.isEmpty()) {
            Orders currentOrder = orders.get(orders.size() - 1);
            vo.setCurrentOrderId(currentOrder.getId());
            vo.setCurrentOrderNo(currentOrder.getNumber());
        }
        return vo;
    }

    private List<DiningOrderRoundVO> buildOrderRounds(List<Orders> orders) {
        List<DiningOrderRoundVO> rounds = new ArrayList<>();
        if (CollectionUtils.isEmpty(orders)) {
            return rounds;
        }
        int displayRound = 1;
        for (Orders order : orders) {
            List<OrderDetail> details = orderDetailMapper.getByOrderId(order.getId());
            if (CollectionUtils.isEmpty(details)) {
                continue;
            }
            Map<Integer, List<OrderDetail>> grouped = new TreeMap<>();
            for (OrderDetail detail : details) {
                int roundNo = detail.getDiningRound() == null || detail.getDiningRound() < 1
                        ? 1 : detail.getDiningRound();
                grouped.computeIfAbsent(roundNo, key -> new ArrayList<>()).add(detail);
            }
            for (List<OrderDetail> items : grouped.values()) {
                DiningOrderRoundVO round = new DiningOrderRoundVO();
                round.setRound(displayRound++);
                round.setOrderId(order.getId());
                round.setOrderNumber(order.getNumber());
                round.setOrderTime(order.getOrderTime());
                round.setAmount(items.stream()
                        .map(item -> (item.getAmount() == null ? BigDecimal.ZERO : item.getAmount())
                                .multiply(BigDecimal.valueOf(item.getNumber() == null ? 0 : item.getNumber())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
                round.setItems(items);
                rounds.add(round);
            }
        }
        return rounds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningSessionVO openTable(DiningOpenTableDTO dto) {
        Long userId = requireUser();
        return doOpenTable(dto, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningSessionVO openTableByAdmin(DiningOpenTableDTO dto) {
        return doOpenTable(dto, 0L);
    }

    private DiningSessionVO doOpenTable(DiningOpenTableDTO dto, Long ownerUserId) {
        if (dto == null || dto.getTableId() == null) {
            throw new BadRequestException("桌台id不能为空");
        }
        DiningTable table = requireTable(dto.getTableId());
        if (table.getStatus() != DiningTable.FREE) {
            throw new BizIllegalException("桌台【" + table.getName() + "】正在使用中");
        }
        if (activeSessionOfTable(table.getId()) != null) {
            throw new BizIllegalException("桌台存在未结束会话");
        }
        if (dto.getReservationId() != null) {
            DiningReservation reservation = requireReservation(dto.getReservationId());
            if (ownerUserId != null && ownerUserId > 0
                    && !Objects.equals(reservation.getUserId(), ownerUserId)) {
                throw new ForbiddenException("只能使用自己的预约开台");
            }
            if (!Objects.equals(reservation.getTableId(), table.getId())) {
                throw new BizIllegalException("预约桌台与开台桌台不一致");
            }
            if (reservation.getStatus() != 1 && reservation.getStatus() != 2 && reservation.getStatus() != 3) {
                throw new BizIllegalException("预约已取消或已过期");
            }
            reservation.setStatus(3);
            reservationMapper.updateById(reservation);
        }
        DiningSession session = DiningSession.builder()
                .sessionNo(newSessionNo())
                .tableId(table.getId())
                .tableName(table.getName())
                .ownerUserId(ownerUserId)
                .guestCount(dto.getGuestCount())
                .status(DiningSession.OPEN)
                .totalAmount(BigDecimal.ZERO)
                .aaEnabled(0)
                .remark(dto.getRemark())
                .openTime(LocalDateTime.now())
                .build();
        sessionMapper.insert(session);

        DiningSessionTable mapping = DiningSessionTable.builder()
                .sessionId(session.getId())
                .tableId(table.getId())
                .isPrimary(1)
                .build();
        sessionTableMapper.insert(mapping);

        table.setStatus(DiningTable.OCCUPIED);
        tableMapper.updateById(table);

        if (ownerUserId != null && ownerUserId > 0) {
            DiningSessionMember member = DiningSessionMember.builder()
                    .sessionId(session.getId())
                    .userId(ownerUserId)
                    .nickname(nicknameOf(ownerUserId, dto.getNickname()))
                    .isOwner(1)
                    .paidAmount(BigDecimal.ZERO)
                    .payStatus(0)
                    .joinTime(LocalDateTime.now())
                    .build();
            memberMapper.insert(member);
        }
        recordOperation(session.getId(), null, table.getId(), 3, "开台");
        return detail(session.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningSessionVO joinSession(DiningJoinDTO dto) {
        Long userId = requireUser();
        if (dto == null || dto.getSessionId() == null) {
            throw new BadRequestException("会话id不能为空");
        }
        DiningSession session = requireActiveSession(dto.getSessionId());
        DiningSessionMember exists = memberMapper.selectOne(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getSessionId, session.getId())
                .eq(DiningSessionMember::getUserId, userId));
        if (exists == null) {
            DiningSessionMember member = DiningSessionMember.builder()
                    .sessionId(session.getId())
                    .userId(userId)
                    .nickname(nicknameOf(userId, dto.getNickname()))
                    .isOwner(0)
                    .paidAmount(BigDecimal.ZERO)
                    .payStatus(0)
                    .joinTime(LocalDateTime.now())
                    .build();
            memberMapper.insert(member);
        } else if (StringUtils.hasText(dto.getNickname())) {
            exists.setNickname(dto.getNickname());
            memberMapper.updateById(exists);
        }
        return detail(session.getId());
    }

    @Override
    public DiningSessionVO myActiveSession() {
        Long userId = requireUser();
        List<DiningSessionMember> members = memberMapper.selectList(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getUserId, userId)
                .orderByDesc(DiningSessionMember::getJoinTime));
        for (DiningSessionMember member : members) {
            DiningSession session = sessionMapper.selectById(member.getSessionId());
            if (session != null && (session.getStatus() == DiningSession.OPEN
                    || session.getStatus() == DiningSession.WAIT_CHECKOUT)) {
                return detail(session.getId());
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> scanTable(String qrToken) {
        DiningTable table = tableMapper.selectOne(Wrappers.<DiningTable>lambdaQuery()
                .eq(DiningTable::getQrToken, qrToken));
        if (table == null) {
            throw new NotFoundException("二维码无效或桌台已删除");
        }
        DiningTableVO tableVO = toTableVO(table);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("table", tableVO);
        if (tableVO.getCurrentSessionId() != null) {
            result.put("session", summary(tableVO.getCurrentSessionId()));
        }
        return result;
    }

    /**
     * 扫码场景只返回会话摘要，不暴露成员、订单、金额等隐私数据。
     */
    private DiningSessionVO summary(Long sessionId) {
        DiningSession session = requireSession(sessionId);
        DiningSessionVO vo = new DiningSessionVO();
        vo.setId(session.getId());
        vo.setSessionNo(session.getSessionNo());
        vo.setTableId(session.getTableId());
        vo.setTableName(session.getTableName());
        vo.setStatus(session.getStatus());
        vo.setGuestCount(session.getGuestCount());
        vo.setAaEnabled(session.getAaEnabled());
        vo.setOpenTime(session.getOpenTime());
        List<String> tableNames = new ArrayList<>();
        for (DiningSessionTable mapping : sessionTableMapper.selectList(Wrappers.<DiningSessionTable>lambdaQuery()
                .eq(DiningSessionTable::getSessionId, sessionId))) {
            DiningTable table = tableMapper.selectById(mapping.getTableId());
            if (table != null) {
                tableNames.add(table.getName());
            }
        }
        vo.setTableNames(tableNames);
        vo.setMemberCount(Math.toIntExact(memberMapper.selectCount(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getSessionId, sessionId))));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableAa(Long sessionId) {
        DiningSession session = requireActiveSession(sessionId);
        requireMember(sessionId, requireUser());
        if (session.getStatus() != DiningSession.OPEN && session.getStatus() != DiningSession.WAIT_CHECKOUT) {
            throw new BizIllegalException("当前会话不能开启 AA");
        }
        if (session.getTotalAmount() == null || session.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizIllegalException("会话还没有消费金额");
        }
        session.setAaEnabled(1);
        session.setStatus(DiningSession.WAIT_CHECKOUT);
        sessionMapper.updateById(session);
        tableMapper.update(null, Wrappers.<DiningTable>lambdaUpdate()
                .in(DiningTable::getId, tableIdsOfSession(sessionId))
                .set(DiningTable::getStatus, DiningTable.WAIT_CHECKOUT));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningSessionVO requestCheckout(Long sessionId) {
        DiningSession session = requireActiveSession(sessionId);
        requireMember(sessionId, requireUser());
        if (session.getStatus() == DiningSession.WAIT_CHECKOUT) {
            return detail(sessionId);
        }
        if (session.getStatus() != DiningSession.OPEN) {
            throw new BizIllegalException("当前会话不能申请结账");
        }
        session.setStatus(DiningSession.WAIT_CHECKOUT);
        sessionMapper.updateById(session);
        tableMapper.update(null, Wrappers.<DiningTable>lambdaUpdate()
                .in(DiningTable::getId, tableIdsOfSession(sessionId))
                .set(DiningTable::getStatus, DiningTable.WAIT_CHECKOUT));
        return detail(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferTable(Long sessionId, DiningTransferDTO dto) {
        DiningSession session = requireActiveSession(sessionId);
        if (dto == null || dto.getTargetTableId() == null) {
            throw new BadRequestException("目标桌台不能为空");
        }
        List<DiningSessionTable> mappings = sessionTableMapper.selectList(Wrappers.<DiningSessionTable>lambdaQuery()
                .eq(DiningSessionTable::getSessionId, sessionId));
        if (mappings.size() != 1) {
            throw new BizIllegalException("并桌后的会话不支持转台，请先拆桌或结账后重新开台");
        }
        DiningTable target = requireTable(dto.getTargetTableId());
        if (target.getStatus() != DiningTable.FREE || activeSessionOfTable(target.getId()) != null) {
            throw new BizIllegalException("目标桌台不是空闲状态");
        }
        DiningTable old = tableMapper.selectById(session.getTableId());
        DiningSessionTable mapping = mappings.get(0);
        mapping.setTableId(target.getId());
        mapping.setIsPrimary(1);
        sessionTableMapper.updateById(mapping);

        target.setStatus(session.getStatus() == DiningSession.WAIT_CHECKOUT
                ? DiningTable.WAIT_CHECKOUT : DiningTable.OCCUPIED);
        tableMapper.updateById(target);
        if (old != null) {
            old.setStatus(DiningTable.FREE);
            tableMapper.updateById(old);
        }
        Long fromTableId = session.getTableId();
        session.setTableId(target.getId());
        session.setTableName(target.getName());
        sessionMapper.updateById(session);
        recordOperation(sessionId, fromTableId, target.getId(), 1, dto.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeSessions(Long targetSessionId, DiningMergeDTO dto) {
        DiningSession target = requireActiveSession(targetSessionId);
        if (dto == null || CollectionUtils.isEmpty(dto.getSourceSessionIds())) {
            throw new BadRequestException("被并入的会话不能为空");
        }
        if (target.getStatus() != DiningSession.OPEN) {
            throw new BizIllegalException("只有就餐中的会话可以并桌");
        }
        List<Long> targetTables = tableIdsOfSession(targetSessionId);
        BigDecimal added = BigDecimal.ZERO;
        for (Long sourceId : dto.getSourceSessionIds()) {
            if (Objects.equals(sourceId, targetSessionId)) {
                continue;
            }
            DiningSession source = requireSession(sourceId);
            if (source.getStatus() != DiningSession.OPEN) {
                throw new BizIllegalException("会话【" + source.getSessionNo() + "】不是就餐中状态");
            }
            if (source.getAaEnabled() != null && source.getAaEnabled() == 1) {
                throw new BizIllegalException("已开启 AA 的会话不能并桌");
            }
            List<DiningSessionTable> sourceMappings = sessionTableMapper.selectList(
                    Wrappers.<DiningSessionTable>lambdaQuery().eq(DiningSessionTable::getSessionId, sourceId));
            for (DiningSessionTable mapping : sourceMappings) {
                DiningSessionTable moved = DiningSessionTable.builder()
                        .sessionId(targetSessionId)
                        .tableId(mapping.getTableId())
                        .isPrimary(0)
                        .build();
                sessionTableMapper.insert(moved);
                DiningTable table = tableMapper.selectById(mapping.getTableId());
                if (table != null) {
                    table.setStatus(DiningTable.OCCUPIED);
                    tableMapper.updateById(table);
                }
            }
            sessionTableMapper.delete(Wrappers.<DiningSessionTable>lambdaQuery()
                    .eq(DiningSessionTable::getSessionId, sourceId));

            ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                    .eq(Orders::getDiningSessionId, sourceId)
                    .set(Orders::getDiningSessionId, targetSessionId));
            shoppingCartService.lambdaUpdate()
                    .eq(ShoppingCart::getDiningSessionId, sourceId)
                    .set(ShoppingCart::getDiningSessionId, targetSessionId);

            List<DiningSessionMember> sourceMembers = memberMapper.selectList(
                    Wrappers.<DiningSessionMember>lambdaQuery().eq(DiningSessionMember::getSessionId, sourceId));
            for (DiningSessionMember member : sourceMembers) {
                DiningSessionMember exists = memberMapper.selectOne(Wrappers.<DiningSessionMember>lambdaQuery()
                        .eq(DiningSessionMember::getSessionId, targetSessionId)
                        .eq(DiningSessionMember::getUserId, member.getUserId()));
                if (exists != null) {
                    memberMapper.deleteById(member.getId());
                } else {
                    member.setSessionId(targetSessionId);
                    member.setIsOwner(0);
                    memberMapper.updateById(member);
                }
            }

            added = added.add(source.getTotalAmount() == null ? BigDecimal.ZERO : source.getTotalAmount());
            source.setStatus(DiningSession.CANCELLED);
            source.setTotalAmount(BigDecimal.ZERO);
            source.setRemark((source.getRemark() == null ? "" : source.getRemark())
                    + " [已并入会话 " + target.getSessionNo() + "]");
            sessionMapper.updateById(source);
            recordOperation(targetSessionId, source.getTableId(), target.getTableId(), 2,
                    "并桌 " + source.getSessionNo() + " -> " + target.getSessionNo());
            targetTables.addAll(sourceMappings.stream().map(DiningSessionTable::getTableId).toList());
        }
        target.setTotalAmount((target.getTotalAmount() == null ? BigDecimal.ZERO : target.getTotalAmount()).add(added));
        target.setTableName(tableNames(targetTables));
        sessionMapper.updateById(target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSession(Long sessionId, String reason) {
        DiningSession session = requireSession(sessionId);
        if (session.getStatus() == DiningSession.CHECKED_OUT || session.getStatus() == DiningSession.CANCELLED) {
            throw new BizIllegalException("会话已结束，不能取消");
        }
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getDiningSessionId, sessionId));
        for (Orders order : orders) {
            // 关闭会话只取消仍在进行中的订单；已完成、已取消或已支付的订单保留，避免覆盖结账结果
            if (order.getStatus() == Orders.CANCELLED || order.getStatus() == Orders.COMPLETED
                    || (order.getPayStatus() != null && order.getPayStatus() == Orders.PAID)) {
                continue;
            }
            ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                    .eq(Orders::getId, order.getId())
                    .set(Orders::getStatus, Orders.CANCELLED)
                    .set(Orders::getCancelReason, StringUtils.hasText(reason) ? reason : "管理员关闭堂食会话")
                    .set(Orders::getCancelTime, LocalDateTime.now()));
            List<OrderDetail> details = orderDetailMapper.getByOrderId(order.getId());
            for (OrderDetail detail : details) {
                dishStockManager.restore(detail.getDishId(), detail.getNumber());
            }
        }
        session.setStatus(DiningSession.CANCELLED);
        session.setRemark((session.getRemark() == null ? "" : session.getRemark())
                + (StringUtils.hasText(reason) ? " [关闭:" + reason + "]" : " [关闭]"));
        sessionMapper.updateById(session);
        releaseTables(sessionId);
        // 关闭会话后清空共享购物车，避免残留菜品被下一桌误用
        shoppingCartService.lambdaUpdate()
                .eq(ShoppingCart::getDiningSessionId, sessionId)
                .remove();
        recordOperation(sessionId, session.getTableId(), null, 5, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> aaPay(DiningAaPayDTO dto) {
        Long userId = requireUser();
        if (dto == null || dto.getSessionId() == null) {
            throw new BadRequestException("会话id不能为空");
        }
        DiningSession session = requireActiveSession(dto.getSessionId());
        DiningSessionMember member = requireMember(dto.getSessionId(), userId);
        if (session.getAaEnabled() == null || session.getAaEnabled() != 1) {
            throw new BizIllegalException("该会话未开启 AA 结账");
        }
        if (session.getStatus() != DiningSession.WAIT_CHECKOUT) {
            throw new BizIllegalException("请先申请结账后再 AA 支付");
        }
        if (member.getPayStatus() != null && member.getPayStatus() == 1) {
            throw new BizIllegalException("你已经支付过了");
        }
        List<DiningSessionMember> members = memberMapper.selectList(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getSessionId, session.getId()));
        BigDecimal total = session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount();
        BigDecimal paid = members.stream()
                .map(m -> m.getPaidAmount() == null ? BigDecimal.ZERO : m.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long unpaid = members.stream().filter(m -> m.getPayStatus() == null || m.getPayStatus() == 0).count();
        BigDecimal share = unpaid <= 1
                ? total.subtract(paid).max(BigDecimal.ZERO)
                : total.subtract(paid).max(BigDecimal.ZERO)
                .divide(BigDecimal.valueOf(unpaid), 2, RoundingMode.HALF_UP);
        member.setPaidAmount(share);
        member.setPayStatus(1);
        memberMapper.updateById(member);
        DiningPayment payment = DiningPayment.builder()
                .sessionId(session.getId())
                .userId(userId)
                .amount(share)
                .payMethod(dto.getPayMethod() == null ? 4 : dto.getPayMethod())
                .payStatus(1)
                .payTime(LocalDateTime.now())
                .remark("AA 分摊")
                .build();
        paymentMapper.insert(payment);

        boolean allPaid = memberMapper.selectCount(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getSessionId, session.getId())
                .eq(DiningSessionMember::getPayStatus, 0)) == 0;
        if (allPaid) {
            finishSession(session, dto.getPayMethod() == null ? 4 : dto.getPayMethod(), "AA 全部结清");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("share", share);
        result.put("paidAmount", member.getPaidAmount());
        result.put("totalAmount", total);
        result.put("allPaid", allPaid);
        result.put("sessionStatus", allPaid ? DiningSession.CHECKED_OUT : DiningSession.WAIT_CHECKOUT);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkout(DiningCheckoutDTO dto) {
        if (dto == null || dto.getSessionId() == null) {
            throw new BadRequestException("会话id不能为空");
        }
        DiningSession session = requireSession(dto.getSessionId());
        if (session.getStatus() != DiningSession.OPEN && session.getStatus() != DiningSession.WAIT_CHECKOUT) {
            throw new BizIllegalException("会话已结账或已取消");
        }
        if (session.getAaEnabled() != null && session.getAaEnabled() == 1
                && !Boolean.TRUE.equals(dto.getSettleUnpaid())) {
            long unpaid = memberMapper.selectCount(Wrappers.<DiningSessionMember>lambdaQuery()
                    .eq(DiningSessionMember::getSessionId, session.getId())
                    .eq(DiningSessionMember::getPayStatus, 0));
            if (unpaid > 0) {
                throw new BizIllegalException("还有 " + unpaid + " 人未完成 AA 支付，可勾选强制结清");
            }
        }
        BigDecimal total = session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount();
        BigDecimal paid = memberMapper.selectList(Wrappers.<DiningSessionMember>lambdaQuery()
                        .eq(DiningSessionMember::getSessionId, session.getId()))
                .stream().map(m -> m.getPaidAmount() == null ? BigDecimal.ZERO : m.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unpaidAmount = total.subtract(paid).max(BigDecimal.ZERO);
        if (session.getAaEnabled() != null && session.getAaEnabled() == 1) {
            memberMapper.update(null, Wrappers.<DiningSessionMember>lambdaUpdate()
                    .eq(DiningSessionMember::getSessionId, session.getId())
                    .set(DiningSessionMember::getPayStatus, 1));
            if (unpaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                insertPayment(session, unpaidAmount, dto.getPayMethod(), dto.getRemark());
            }
        } else {
            insertPayment(session, total, dto.getPayMethod(), dto.getRemark());
            markMembersPaid(session, total);
        }
        finishSession(session, dto.getPayMethod(), dto.getRemark());
    }

    // ==================== 共享购物车 / 堂食下单 ====================

    @Override
    public List<ShoppingCart> listCart(Long sessionId) {
        requireActiveSession(sessionId);
        requireMember(sessionId, requireUser());
        return shoppingCartService.lambdaQuery()
                .eq(ShoppingCart::getDiningSessionId, sessionId)
                .orderByAsc(ShoppingCart::getId)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCartItem(DiningCartDTO dto) {
        Long userId = requireUser();
        if (dto == null || dto.getSessionId() == null || dto.getDishId() == null || dto.getNumber() == null) {
            throw new BadRequestException("加菜参数不完整");
        }
        DiningSession session = requireActiveSession(dto.getSessionId());
        requireMember(dto.getSessionId(), userId);
        if (session.getStatus() != DiningSession.OPEN) {
            throw new BizIllegalException("已申请结账，不能继续加菜");
        }
        Dish dish = dishService.getById(dto.getDishId());
        if (dish == null) {
            throw new NotFoundException("菜品不存在");
        }
        if (dish.getStatus() == null || dish.getStatus() != 1) {
            throw new BizIllegalException("菜品【" + dish.getName() + "】已停售");
        }
        List<ShoppingCart> existedList = shoppingCartService.lambdaQuery()
                .eq(ShoppingCart::getDiningSessionId, dto.getSessionId())
                .eq(ShoppingCart::getDishId, dto.getDishId())
                .eq(StringUtils.hasText(dto.getDishFlavor()), ShoppingCart::getDishFlavor, dto.getDishFlavor())
                .isNull(!StringUtils.hasText(dto.getDishFlavor()), ShoppingCart::getDishFlavor)
                .orderByAsc(ShoppingCart::getId)
                .list();
        ShoppingCart exists = existedList.isEmpty() ? null : existedList.get(0);
        if (exists == null) {
            if (dto.getNumber() <= 0) {
                return;
            }
            ShoppingCart cart = ShoppingCart.builder()
                    .name(dish.getName())
                    .image(dish.getImage())
                    .userId(userId)
                    .dishId(dish.getId())
                    .dishFlavor(dto.getDishFlavor())
                    .number(dto.getNumber())
                    .amount(dish.getPrice())
                    .diningSessionId(dto.getSessionId())
                    .createTime(LocalDateTime.now())
                    .build();
            shoppingCartService.save(cart);
        } else {
            int newNumber = (exists.getNumber() == null ? 0 : exists.getNumber()) + dto.getNumber();
            if (newNumber <= 0) {
                shoppingCartService.removeById(exists.getId());
            } else {
                exists.setNumber(newNumber);
                exists.setAmount(dish.getPrice());
                exists.setUserId(userId);
                shoppingCartService.updateById(exists);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCartItem(Long sessionId, Long cartId) {
        requireActiveSession(sessionId);
        requireMember(sessionId, requireUser());
        ShoppingCart cart = shoppingCartService.getById(cartId);
        if (cart == null || !Objects.equals(cart.getDiningSessionId(), sessionId)) {
            throw new NotFoundException("购物车项不存在");
        }
        shoppingCartService.removeById(cartId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long sessionId) {
        requireActiveSession(sessionId);
        requireMember(sessionId, requireUser());
        shoppingCartService.lambdaUpdate()
                .eq(ShoppingCart::getDiningSessionId, sessionId)
                .remove();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitDiningOrder(Long sessionId) {
        Long userId = requireUser();
        orderRateLimiter.check(userId);
        DiningSession session = requireActiveSession(sessionId);
        requireMember(sessionId, userId);
        if (session.getStatus() != DiningSession.OPEN) {
            throw new BizIllegalException("已申请结账，不能继续加菜");
        }
        List<ShoppingCart> carts = shoppingCartService.lambdaQuery()
                .eq(ShoppingCart::getDiningSessionId, sessionId)
                .list();
        if (CollectionUtils.isEmpty(carts)) {
            throw new BizIllegalException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        BigDecimal amount = carts.stream()
                .map(c -> (c.getAmount() == null ? BigDecimal.ZERO : c.getAmount())
                        .multiply(BigDecimal.valueOf(c.getNumber() == null ? 0 : c.getNumber())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<DishStockManager.StockItem> stockItems = carts.stream()
                .filter(c -> c.getDishId() != null && c.getNumber() != null && c.getNumber() > 0)
                .map(c -> new DishStockManager.StockItem(c.getDishId(), c.getNumber(), c.getName()))
                .toList();
        dishStockManager.deductDishes(stockItems);

        // 同一堂食会话只复用一笔进行中的订单；第一次下单创建订单，后续加餐只追加明细。
        Orders currentOrder = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getDiningSessionId, sessionId)
                .eq(Orders::getOrderType, Orders.TYPE_DINING)
                .in(Orders::getStatus, Orders.PENDING_PAYMENT, Orders.TO_BE_CONFIRMED, Orders.CONFIRMED)
                .orderByDesc(Orders::getOrderTime)
                .orderByDesc(Orders::getId)
                .last("LIMIT 1"));
        boolean newOrder = currentOrder == null;
        int diningRound = 1;
        try {
            if (newOrder) {
                currentOrder = Orders.builder()
                        .number("D" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                                + String.format("%03d", (int) (Math.random() * 1000)))
                        .status(shopSettingsService.isDiningAutoAccept()
                                ? Orders.CONFIRMED : Orders.TO_BE_CONFIRMED)
                        .userId(userId)
                        .orderTime(LocalDateTime.now())
                        .payMethod(0)
                        .payStatus(Orders.UN_PAID)
                        .amount(amount)
                        .remark("堂食-" + session.getTableName())
                        .userName(nicknameOf(userId, null))
                        .packAmount(0)
                        .tablewareNumber(0)
                        .tablewareStatus(0)
                        .orderType(Orders.TYPE_DINING)
                        .diningSessionId(sessionId)
                        .build();
                ordersMapper.insert(currentOrder);
            } else {
                List<OrderDetail> existingDetails = orderDetailMapper.getByOrderId(currentOrder.getId());
                diningRound = existingDetails.stream()
                        .map(detail -> detail.getDiningRound() == null || detail.getDiningRound() < 1
                                ? 1 : detail.getDiningRound())
                        .max(Integer::compareTo)
                        .orElse(0) + 1;
                BigDecimal totalAmount = (currentOrder.getAmount() == null ? BigDecimal.ZERO : currentOrder.getAmount())
                        .add(amount);
                currentOrder.setAmount(totalAmount);
                ordersMapper.updateById(currentOrder);
            }

            List<OrderDetail> details = new ArrayList<>();
            for (ShoppingCart cart : carts) {
                OrderDetail detail = new OrderDetail();
                BeanUtils.copyProperties(cart, detail);
                detail.setId(null);
                detail.setOrderId(currentOrder.getId());
                detail.setDiningRound(diningRound);
                details.add(detail);
            }
            orderDetailMapper.insertBatch(details);
            shoppingCartService.lambdaUpdate()
                    .eq(ShoppingCart::getDiningSessionId, sessionId)
                    .remove();
            session.setTotalAmount((session.getTotalAmount() == null ? BigDecimal.ZERO : session.getTotalAmount())
                    .add(amount));
            sessionMapper.updateById(session);
        } catch (RuntimeException e) {
            stockItems.forEach(item -> dishStockManager.restore(item.dishId(), item.number()));
            throw e;
        }
        return OrderSubmitVO.builder()
                .id(currentOrder.getId())
                .orderNumber(currentOrder.getNumber())
                .orderAmount(amount)
                .orderTime(LocalDateTime.now())
                .diningRound(diningRound)
                .build();
    }


    // ==================== 报表 ====================
    @Override
    public DiningReportVO report(LocalDateTime begin, LocalDateTime end) {
        LocalDateTime start = begin == null ? LocalDate.now().atStartOfDay() : begin;
        LocalDateTime finish = end == null ? LocalDate.now().plusDays(1).atStartOfDay() : end;
        if (start.isAfter(finish)) {
            throw new BadRequestException("开始时间不能晚于结束时间");
        }
        long count = sessionMapper.countCheckedOut(start, finish);
        BigDecimal revenue = sessionMapper.sumRevenue(start, finish);
        long guests = sessionMapper.sumGuests(start, finish);
        int tableCount = Math.toIntExact(tableMapper.selectCount(null));
        DiningReportVO vo = new DiningReportVO();
        vo.setSessionCount(count);
        vo.setTotalRevenue(revenue == null ? BigDecimal.ZERO : revenue);
        vo.setTotalGuests(guests);
        vo.setTableCount(tableCount);
        vo.setAvgAmount(count == 0 ? BigDecimal.ZERO
                : vo.getTotalRevenue().divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        vo.setAvgGuests(count == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(guests).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP));
        vo.setTurnoverRate(tableCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(count).divide(BigDecimal.valueOf(tableCount), 2, RoundingMode.HALF_UP));
        vo.setDaily(sessionMapper.dailyReport(start, finish));
        return vo;
    }

    // ==================== 内部方法 ====================

    private void finishSession(DiningSession session, Integer payMethod, String remark) {
        LocalDateTime now = LocalDateTime.now();
        ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                .eq(Orders::getDiningSessionId, session.getId())
                .in(Orders::getStatus, Orders.PENDING_PAYMENT, Orders.TO_BE_CONFIRMED, Orders.CONFIRMED)
                .set(Orders::getStatus, Orders.COMPLETED)
                .set(Orders::getPayStatus, Orders.PAID)
                .set(Orders::getCheckoutTime, now)
                .set(payMethod != null, Orders::getPayMethod, payMethod));
        session.setStatus(DiningSession.CHECKED_OUT);
        session.setCheckoutTime(now);
        sessionMapper.updateById(session);
        releaseTables(session.getId());
        // 结账后清空共享购物车；会话成员保留作 AA/支付历史，但会话已关闭，客户端会退出会话
        shoppingCartService.lambdaUpdate()
                .eq(ShoppingCart::getDiningSessionId, session.getId())
                .remove();
        recordOperation(session.getId(), session.getTableId(), null, 4, remark);
    }

    private void releaseTables(Long sessionId) {
        List<Long> tableIds = tableIdsOfSession(sessionId);
        if (!tableIds.isEmpty()) {
            tableMapper.update(null, Wrappers.<DiningTable>lambdaUpdate()
                    .in(DiningTable::getId, tableIds)
                    .set(DiningTable::getStatus, DiningTable.FREE));
        }
    }

    private void markMembersPaid(DiningSession session, BigDecimal total) {
        List<DiningSessionMember> members = memberMapper.selectList(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getSessionId, session.getId()));
        for (DiningSessionMember member : members) {
            member.setPayStatus(1);
            if (member.getIsOwner() != null && member.getIsOwner() == 1) {
                member.setPaidAmount(total);
            } else {
                member.setPaidAmount(BigDecimal.ZERO);
            }
            memberMapper.updateById(member);
        }
    }

    private void insertPayment(DiningSession session, BigDecimal amount, Integer payMethod, String remark) {
        DiningPayment payment = DiningPayment.builder()
                .sessionId(session.getId())
                .userId(session.getOwnerUserId())
                .amount(amount)
                .payMethod(payMethod == null ? 3 : payMethod)
                .payStatus(1)
                .payTime(LocalDateTime.now())
                .remark(remark)
                .build();
        paymentMapper.insert(payment);
    }

    private void recordOperation(Long sessionId, Long fromTableId, Long toTableId,
                                 Integer type, String remark) {
        DiningTableOperation operation = DiningTableOperation.builder()
                .sessionId(sessionId)
                .fromTableId(fromTableId)
                .toTableId(toTableId)
                .operationType(type)
                .operatorId(UserContext.getCurrentId())
                .operatorType(UserContext.isAdmin() ? 1 : (UserContext.getCurrentId() == null ? 3 : 2))
                .remark(remark)
                .build();
        operationMapper.insert(operation);
    }

    private DiningTableVO toTableVO(DiningTable table) {
        DiningTableVO vo = new DiningTableVO();
        BeanUtils.copyProperties(table, vo);
        vo.setQrContent("DINING_TABLE:" + table.getQrToken());
        DiningSession session = activeSessionOfTable(table.getId());
        if (session != null) {
            vo.setCurrentSessionId(session.getId());
            vo.setCurrentSessionNo(session.getSessionNo());
        }
        return vo;
    }

    private DiningSession activeSessionOfTable(Long tableId) {
        List<DiningSessionTable> mappings = sessionTableMapper.selectList(Wrappers.<DiningSessionTable>lambdaQuery()
                .eq(DiningSessionTable::getTableId, tableId)
                .orderByDesc(DiningSessionTable::getId));
        for (DiningSessionTable mapping : mappings) {
            DiningSession session = sessionMapper.selectById(mapping.getSessionId());
            if (session != null && (session.getStatus() == DiningSession.OPEN
                    || session.getStatus() == DiningSession.WAIT_CHECKOUT)) {
                return session;
            }
        }
        return null;
    }

    private List<Long> tableIdsOfSession(Long sessionId) {
        return sessionTableMapper.selectList(Wrappers.<DiningSessionTable>lambdaQuery()
                        .eq(DiningSessionTable::getSessionId, sessionId))
                .stream().map(DiningSessionTable::getTableId).collect(Collectors.toList());
    }

    private String tableNames(List<Long> tableIds) {
        if (tableIds.isEmpty()) {
            return null;
        }
        return tableMapper.selectList(Wrappers.<DiningTable>lambdaQuery()
                        .in(DiningTable::getId, tableIds))
                .stream().map(DiningTable::getName).collect(Collectors.joining("+"));
    }

    private void expireReservations() {
        reservationMapper.update(null, Wrappers.<DiningReservation>lambdaUpdate()
                .in(DiningReservation::getStatus, 1, 2)
                .lt(DiningReservation::getReserveTime, LocalDateTime.now().minusMinutes(30))
                .set(DiningReservation::getStatus, 5));
    }

    private DiningTable requireTable(Long id) {
        if (id == null) {
            throw new BadRequestException("桌台id不能为空");
        }
        DiningTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new NotFoundException("桌台不存在");
        }
        return table;
    }

    private DiningSession requireSession(Long id) {
        if (id == null) {
            throw new BadRequestException("会话id不能为空");
        }
        DiningSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new NotFoundException("堂食会话不存在");
        }
        return session;
    }

    private DiningSession requireActiveSession(Long id) {
        DiningSession session = requireSession(id);
        if (session.getStatus() != DiningSession.OPEN && session.getStatus() != DiningSession.WAIT_CHECKOUT) {
            throw new BizIllegalException("会话已结束");
        }
        return session;
    }

    private DiningReservation requireReservation(Long id) {
        if (id == null) {
            throw new BadRequestException("预约id不能为空");
        }
        DiningReservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            throw new NotFoundException("预约不存在");
        }
        return reservation;
    }

    private DiningSessionMember requireMember(Long sessionId, Long userId) {
        DiningSessionMember member = memberMapper.selectOne(Wrappers.<DiningSessionMember>lambdaQuery()
                .eq(DiningSessionMember::getSessionId, sessionId)
                .eq(DiningSessionMember::getUserId, userId));
        if (member == null) {
            throw new ForbiddenException("你不在该堂食会话中，请先扫码加入");
        }
        return member;
    }

    private Long requireUser() {
        Long userId = UserContext.getCurrentId();
        if (userId == null || UserContext.isAdmin()) {
            throw new BadRequestException(MessageConstant.USER_NOT_LOGIN);
        }
        return userId;
    }

    private String nicknameOf(Long userId, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        try {
            User user = userService.getById(userId);
            if (user != null && StringUtils.hasText(user.getName())) {
                return user.getName();
            }
        } catch (Exception e) {
            log.warn("查询用户昵称失败 userId={}: {}", userId, e.getMessage());
        }
        return "用户" + userId;
    }

    private String newQrToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String newSessionNo() {
        return "D" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%03d", (int) (Math.random() * 1000));
    }

}




