package com.mzlhsc.orpl.user.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mzlhsc.orpl.user.domain.po.AddressBook;
import com.mzlhsc.orpl.user.mapper.AddressBookMapper;
import com.mzlhsc.orpl.user.service.IAddressBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 地址簿 服务实现类
 * </p>
 *
 * @author mzlhsc
 * @since 2026-11-13
 */
@Service
@RequiredArgsConstructor
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements IAddressBookService {

}
