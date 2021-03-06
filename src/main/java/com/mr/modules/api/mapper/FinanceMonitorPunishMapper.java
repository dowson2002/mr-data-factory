package com.mr.modules.api.mapper;

import com.mr.common.base.mapper.BaseMapper;
import com.mr.modules.api.model.FinanceMonitorPunish;

public interface FinanceMonitorPunishMapper extends BaseMapper<FinanceMonitorPunish> {
	/**
	 * 通过业务主键删除
	 *
	 * @param primaryKey
	 * @return
	 */
	int deleteByBizKey(String primaryKey);

	/**
	 * 通过业务主键查找
	 */
	FinanceMonitorPunish selectByBizKey(String primaryKey);

	/**
	 * 通过链接地址删除
	 * @param source
	 * @return
	 */
	int deleteBySource(String source);

	/**
	 * 通过链接地址查找
	 * @param source
	 * @return
	 */
	FinanceMonitorPunish selectBySource(String source);

	/**
	 * 通过业务来源删除
	 * @return
	 */
	int deleteByObject(String object);

}