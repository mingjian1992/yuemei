/*
SQLyog 企业版 - MySQL GUI v8.14 
MySQL - 5.1.57-community : Database - spay7_hntl
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

/*Table structure for table `t_bids` */

DROP TABLE IF EXISTS `t_bids`;

CREATE TABLE `t_bids` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `bid_no` varchar(200) DEFAULT '' COMMENT '标的号',
  `remain_fee` double(10,2) DEFAULT '0.00' COMMENT '剩余借款管理费',
  `is_success` bit(1) DEFAULT b'0' COMMENT '是否发标成功，1是，0否',
  `amount` decimal(20,2) DEFAULT '0.00' COMMENT '借款金额',
  `has_invested_amount` decimal(20,2) DEFAULT '0.00' COMMENT '已投总额(冗余)',
  `version` int(11) DEFAULT '0' COMMENT '版本--（用于控制并发）',
  `bid_id` bigint(20) DEFAULT NULL COMMENT '标的ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='标的记录（双乾专用）';

/*Table structure for table `t_consumers` */

DROP TABLE IF EXISTS `t_consumers`;

CREATE TABLE `t_consumers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '注册时间',
  `name` varchar(50) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '管理员帐号',
  `password` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '录登密码',
  `unique_number` varchar(128) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '唯一识别码',
  `secret_key` varchar(128) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '密钥',
  `public_key` varchar(128) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '公钥',
  `group_Id` int(11) DEFAULT NULL COMMENT '接口组id',
  `status` bit(1) DEFAULT b'1' COMMENT '状态 0不启用 1启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `unique_number` (`unique_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_hungarian_ci;

/*Table structure for table `t_db_operations` */

DROP TABLE IF EXISTS `t_db_operations`;

CREATE TABLE `t_db_operations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `supervisor_id` bigint(20) DEFAULT NULL COMMENT '管理员id',
  `time` timestamp NULL DEFAULT NULL COMMENT '时间',
  `ip` varchar(255) DEFAULT NULL COMMENT 'ip地址',
  `type` int(11) DEFAULT NULL COMMENT '操作类型\n0 清空数据\n1 还原出厂初始数据\n2 还原运营数据\n3 备份数据',
  `filename` varchar(255) DEFAULT NULL COMMENT '备份文件名',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据库操作记录';

/*Table structure for table `t_dict_ad_areas` */

DROP TABLE IF EXISTS `t_dict_ad_areas`;

CREATE TABLE `t_dict_ad_areas` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '区id',
  `city_id` int(11) DEFAULT NULL COMMENT '市ID',
  `name` varchar(255) DEFAULT NULL COMMENT '市',
  PRIMARY KEY (`id`),
  KEY `city_id` (`city_id`)
) ENGINE=InnoDB AUTO_INCREMENT=659008 DEFAULT CHARSET=utf8 COMMENT='地理位置区信息表';

/*Table structure for table `t_dict_ad_citys` */

DROP TABLE IF EXISTS `t_dict_ad_citys`;

CREATE TABLE `t_dict_ad_citys` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '城市id',
  `province_id` int(11) DEFAULT NULL COMMENT '省ID',
  `name` varchar(255) DEFAULT NULL COMMENT '市',
  PRIMARY KEY (`id`),
  KEY `province_id` (`province_id`),
  KEY `index_city_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=659001 DEFAULT CHARSET=utf8 COMMENT='地理位置城市信息表';

/*Table structure for table `t_dict_ad_provinces` */

DROP TABLE IF EXISTS `t_dict_ad_provinces`;

CREATE TABLE `t_dict_ad_provinces` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL COMMENT '省份',
  PRIMARY KEY (`id`),
  KEY `index_province_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=650001 DEFAULT CHARSET=utf8 COMMENT='地理位置省份信息表';

/*Table structure for table `t_dict_ipaddress_regions` */

DROP TABLE IF EXISTS `t_dict_ipaddress_regions`;

CREATE TABLE `t_dict_ipaddress_regions` (
  `id` bigint(20) NOT NULL COMMENT '地理位置ID',
  `country` varchar(50) DEFAULT NULL COMMENT '国家',
  `province` varchar(50) DEFAULT NULL COMMENT '省份',
  PRIMARY KEY (`id`),
  KEY `index_dict_ip_reg_id` (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='IP位置地区信息表';

/*Table structure for table `t_dict_payment_gateways` */

DROP TABLE IF EXISTS `t_dict_payment_gateways`;

CREATE TABLE `t_dict_payment_gateways` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `_key` varchar(255) DEFAULT NULL,
  `account` varchar(255) DEFAULT NULL,
  `is_use` bit(1) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `pid` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='资金托管账户设置';

/*Table structure for table `t_guo_order_details` */

DROP TABLE IF EXISTS `t_guo_order_details`;

CREATE TABLE `t_guo_order_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `version` varchar(5) DEFAULT NULL COMMENT '网关版本号',
  `charset` varchar(1) DEFAULT NULL COMMENT '字符集',
  `signType` varchar(1) DEFAULT NULL COMMENT '加密方式',
  `tranCode` varchar(4) DEFAULT NULL COMMENT '交易代码',
  `merId` varchar(10) DEFAULT NULL COMMENT '商户代码',
  `merName` varchar(100) DEFAULT NULL COMMENT '商户名称',
  `tranAmt` varchar(12) DEFAULT NULL COMMENT '投资金额',
  `payType` varchar(1) DEFAULT NULL COMMENT '支付方式',
  `feeAmt` varchar(12) DEFAULT NULL COMMENT '国付宝手续费',
  `feePayer` varchar(1) DEFAULT NULL COMMENT '国付宝手续承担方',
  `frontMerUrl` varchar(256) DEFAULT NULL COMMENT '商户前台通知地址',
  `tranDateTime` varchar(14) DEFAULT NULL COMMENT '交易时间',
  `contractNo` varchar(18) DEFAULT NULL COMMENT '提现专属账户的签约协议号',
  `p2pUserId` varchar(32) DEFAULT NULL COMMENT 'P2p用户在国付宝平台的用户ID',
  `virCardNo` varchar(19) DEFAULT NULL COMMENT '国付宝虚拟账号',
  `merOrderNum` varchar(30) DEFAULT NULL COMMENT '订单号',
  `mercFeeAm` varchar(12) DEFAULT NULL COMMENT 'P2P平台佣金',
  `backgroundMerUrl` varchar(256) DEFAULT NULL COMMENT '商户后台通知地址',
  `respCode` varchar(6) DEFAULT NULL COMMENT '响应码',
  `customerId` varchar(20) DEFAULT NULL COMMENT 'P2P平台用户ID',
  `mobilePhone` varchar(11) DEFAULT NULL COMMENT '开通用户的手机号',
  `extantAmt` varchar(12) DEFAULT NULL COMMENT '留存金额',
  `orderId` varchar(16) DEFAULT NULL COMMENT '国付宝内部订单号',
  `bidId` varchar(80) DEFAULT NULL COMMENT '标号',
  `tranFinishTime` varchar(14) DEFAULT NULL COMMENT '交易完成时间',
  `mercFeeAmt` varchar(12) DEFAULT NULL COMMENT 'P2P平台佣金',
  `bankPayAmt` varchar(12) DEFAULT NULL COMMENT '银行卡支付金额',
  `vcardPayAmt` varchar(12) DEFAULT NULL COMMENT '国付宝虚拟卡支付金额',
  `curBal` varchar(12) DEFAULT NULL COMMENT '投资人国付宝虚拟卡可用余额',
  `repaymentType` varchar(1) DEFAULT NULL COMMENT '还款类型',
  `isInFull` varchar(1) DEFAULT NULL COMMENT '是否全额还款',
  `repaymentInfo` varchar(1024) DEFAULT NULL COMMENT '还款信息',
  `repaymentChargeFeeAmt` varchar(12) DEFAULT NULL COMMENT '还款充值手续费',
  `repaymentChargeFeePayer` varchar(1) DEFAULT NULL COMMENT '还款充值手续费承担方',
  `tranIP` varchar(19) DEFAULT NULL COMMENT '用户浏览器IP',
  `signValue` varchar(1024) DEFAULT NULL COMMENT '加密串',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='国付宝异步回调记录表';

/*Table structure for table `t_invests` */

DROP TABLE IF EXISTS `t_invests`;

CREATE TABLE `t_invests` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '投资ID',
  `user_id` bigint(20) DEFAULT '0' COMMENT '用户id(投资人)',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投资时间',
  `bid_id` bigint(20) DEFAULT '0' COMMENT '借款标id',
  `ips_bill_no` varchar(200) DEFAULT NULL COMMENT '第三方支付返回的订单号（资金托管）',
  `amount` decimal(20,2) DEFAULT '0.00' COMMENT '投资金额',
  `bid_no` varchar(200) DEFAULT '' COMMENT '标的编号',
  PRIMARY KEY (`id`),
  KEY `index_inv_id` (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='投资理财信息表';

/*Table structure for table `t_loans_details` */

DROP TABLE IF EXISTS `t_loans_details`;

CREATE TABLE `t_loans_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `memberId` varchar(30) DEFAULT NULL COMMENT '互联网金融平台用户id',
  `bidNo` varchar(30) DEFAULT NULL COMMENT '标的号',
  `oriMerBillNo` varchar(20) DEFAULT NULL COMMENT '原商户订单号',
  `trdAmt` varchar(11) DEFAULT NULL COMMENT '转账金额',
  `fAcctType` varchar(1) DEFAULT NULL COMMENT '转出方账户类型0#机构;1#个人',
  `fIpsAcctNo` varchar(30) DEFAULT NULL COMMENT '转出方托管账户号',
  `fTrdFee` varchar(11) DEFAULT NULL COMMENT '转出方明细手续',
  `tAcctType` varchar(1) DEFAULT NULL COMMENT '转入方账户类型',
  `tIpsAcctNo` varchar(30) DEFAULT NULL COMMENT '转入方托管账户号',
  `tTrdFee` varchar(11) DEFAULT NULL COMMENT '转入方明细手续',
  `ipsBillNo` varchar(20) DEFAULT NULL COMMENT '冻结标识',
  `merBillNo` varchar(30) DEFAULT NULL COMMENT '商户订单号',
  `status` tinyint(1) DEFAULT '0' COMMENT '1成功0失败',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='汇付天下自动放款交易记录存储';

/*Table structure for table `t_member_detail_sequences` */

DROP TABLE IF EXISTS `t_member_detail_sequences`;

CREATE TABLE `t_member_detail_sequences` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `serial_number` varchar(128) CHARACTER SET utf8 DEFAULT NULL COMMENT '流水号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_hungarian_ci COMMENT='交易记录序列';

/*Table structure for table `t_member_detail_types` */

DROP TABLE IF EXISTS `t_member_detail_types`;

CREATE TABLE `t_member_detail_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL COMMENT '名称',
  `type` tinyint(4) DEFAULT NULL COMMENT '1 收入\r\n2 支出\r\n3 冻结\r\n4 解冻',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8 COMMENT='用户资金交易类型信息表';

/*Table structure for table `t_member_details` */

DROP TABLE IF EXISTS `t_member_details`;

CREATE TABLE `t_member_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `member_id` bigint(20) DEFAULT '-1' COMMENT '用户id',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
  `platform_id` int(11) DEFAULT '-1' COMMENT '平台id',
  `serial_number` varchar(128) DEFAULT NULL COMMENT '流水号',
  `operation` int(11) DEFAULT '0' COMMENT '账户收支操作项(1 在线支付 2 手工充值 3 转账入 4 解冻 ... 1001 转账出 1002 冻结...)',
  `amount` decimal(20,2) DEFAULT '0.00' COMMENT '交易金额',
  `status` bit(1) DEFAULT b'0' COMMENT '交易记录状态 0 失败 1成功',
  `summary` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `serial_number` (`serial_number`),
  KEY `index_ud_id` (`id`) USING BTREE,
  KEY `index_ud_usr_id` (`member_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=137 DEFAULT CHARSET=utf8 COMMENT='用户资金交易记录信息表';

/*Table structure for table `t_member_event_types` */

DROP TABLE IF EXISTS `t_member_event_types`;

CREATE TABLE `t_member_event_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `name` varchar(50) DEFAULT NULL COMMENT '事件名称',
  PRIMARY KEY (`id`),
  KEY `index_uet_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8 COMMENT='用户事件类型';

/*Table structure for table `t_member_events` */

DROP TABLE IF EXISTS `t_member_events`;

CREATE TABLE `t_member_events` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `member_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `serial_number` varchar(128) DEFAULT NULL COMMENT '流水号',
  `platform_id` varchar(50) DEFAULT NULL,
  `type_id` int(11) DEFAULT NULL COMMENT '事件类型id',
  `front_url` varchar(200) DEFAULT NULL COMMENT '前台通知（同步）',
  `background_url` varchar(200) DEFAULT NULL COMMENT '后台通知（异步）',
  `remark` text COMMENT '请求的备注信息',
  `descrption` text,
  PRIMARY KEY (`id`),
  KEY `user_id` (`member_id`),
  KEY `index_ue_id` (`member_id`) USING BTREE,
  KEY `index_serial_number` (`serial_number`),
  KEY `index_platform_id` (`platform_id`)
) ENGINE=InnoDB AUTO_INCREMENT=84 DEFAULT CHARSET=utf8 COMMENT='用户事件信息表';

/*Table structure for table `t_member_of_platforms` */

DROP TABLE IF EXISTS `t_member_of_platforms`;

CREATE TABLE `t_member_of_platforms` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `member_id` bigint(20) DEFAULT NULL COMMENT '会员id',
  `platform_id` int(11) DEFAULT NULL COMMENT '平台id',
  `platform_member_id` bigint(20) DEFAULT NULL COMMENT '平台会员id',
  `platform_member_name` varchar(50) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '平台用户名',
  `platform_member_account` varchar(128) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '平台用户支付id',
  `platform_member_account_id` varchar(30) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '资金托管账户唯一标识',
  `auth_payment_number` varchar(50) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '自动还款授权号',
  `auth_invest_number` varchar(50) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '自动投标授权号',
  `card_no` varchar(50) COLLATE utf8_hungarian_ci DEFAULT '' COMMENT '默认提现银行卡卡号(双乾）',
  `card_status` tinyint(4) DEFAULT '0' COMMENT '绑卡状态： 0.未绑卡 1.受理成功 2.认证成功',
  PRIMARY KEY (`id`),
  KEY `platform_id` (`platform_id`),
  KEY `platform_member_id` (`platform_member_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 COLLATE=utf8_hungarian_ci COMMENT='平台会员关系表';

/*Table structure for table `t_members` */

DROP TABLE IF EXISTS `t_members`;

CREATE TABLE `t_members` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '注册时间',
  `name` varchar(50) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '用户帐号',
  `password` varchar(32) CHARACTER SET utf8 DEFAULT NULL COMMENT '录登密码',
  `id_number` varchar(50) CHARACTER SET utf8 DEFAULT NULL COMMENT '身份证号吗',
  `mobile` varchar(50) CHARACTER SET utf8 DEFAULT NULL COMMENT '手机号码',
  `serial_number` varchar(128) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '流水号',
  `status` bit(1) DEFAULT b'1' COMMENT '状态 0不启用 1启用',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `id_number` (`id_number`),
  KEY `mobile` (`mobile`) USING BTREE,
  KEY `serial_number` (`serial_number`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8 COLLATE=utf8_hungarian_ci COMMENT='平台会员';

/*Table structure for table `t_payment_gateways` */

DROP TABLE IF EXISTS `t_payment_gateways`;

CREATE TABLE `t_payment_gateways` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `name` varchar(100) DEFAULT NULL COMMENT '支付接口名称',
  `account` varchar(100) DEFAULT NULL COMMENT '账号',
  `pid` varchar(100) DEFAULT NULL COMMENT '标识码',
  `_key` varchar(100) DEFAULT NULL COMMENT 'KEY或者密钥',
  `information` varchar(2000) DEFAULT NULL COMMENT '支付接口信息',
  `is_use` bit(1) DEFAULT b'1' COMMENT '是否启用(0不启用 1启用)',
  PRIMARY KEY (`id`),
  KEY `index_acc_pay_id` (`id`) USING HASH,
  KEY `index_pid` (`pid`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='第三方支付帐号';

/*Table structure for table `t_platforms` */

DROP TABLE IF EXISTS `t_platforms`;

CREATE TABLE `t_platforms` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '注册时间',
  `name` varchar(50) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '平台名称',
  `domain` varchar(128) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '约定密钥',
  `gateway_id` bigint(20) DEFAULT '-1' COMMENT '第三方支付帐号ID',
  `encryption` varchar(200) COLLATE utf8_hungarian_ci DEFAULT NULL COMMENT '绑定域名',
  `status` bit(1) DEFAULT b'1' COMMENT '状态 0不启用 1启用',
  `use_type` int(11) DEFAULT '1' COMMENT '使用方式：1 本地测试 2 第三方测试',
  `deal_status` bit(1) DEFAULT b'0' COMMENT '是否发生了交易 0 否 1 是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `domain` (`domain`),
  KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8 COLLATE=utf8_hungarian_ci COMMENT='平台管理';

/*Table structure for table `t_repayment_details` */

DROP TABLE IF EXISTS `t_repayment_details`;

CREATE TABLE `t_repayment_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ordId` varchar(20) DEFAULT NULL COMMENT '订单号',
  `ordDate` varchar(8) DEFAULT NULL COMMENT '订单日期',
  `outCustId` varchar(16) DEFAULT NULL COMMENT '出账客户号',
  `subOrdId` varchar(20) DEFAULT NULL COMMENT '订单号',
  `subOrdDate` varchar(8) DEFAULT NULL COMMENT '订单日期',
  `transAmt` varchar(14) DEFAULT NULL COMMENT '交易金额',
  `fee` varchar(12) DEFAULT NULL COMMENT '扣款手续费',
  `inCustId` varchar(16) DEFAULT NULL COMMENT '入账客户号',
  `reqExt` varchar(512) DEFAULT NULL COMMENT '入参扩展域',
  `memberId` varchar(40) DEFAULT NULL COMMENT '互联网金融平台用户id',
  `pBidNo` varchar(30) DEFAULT NULL COMMENT '标的号',
  `pMerBillNo` varchar(30) DEFAULT NULL COMMENT '商户还款订单号',
  `status` bigint(1) DEFAULT '0' COMMENT '状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='汇付天下还款记录';

/*Table structure for table `t_right_actions` */

DROP TABLE IF EXISTS `t_right_actions`;

CREATE TABLE `t_right_actions` (
  `id` int(11) NOT NULL COMMENT '管理首页1-999\n网站内容管理1000-1999\n借款标管理2000-2999\n账单催收3000-3999\n会员管理 4000-4999\n财务管理5000-5999\n平台推广6000-6999\n数据统计7000-7999\n系统设置 8000-8999\nOBU风控联盟9000-9999',
  `right_id` int(11) NOT NULL COMMENT '权限id',
  `action` varchar(255) NOT NULL COMMENT '路由中的action',
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='权限对应路由（action）';

/*Table structure for table `t_right_groups` */

DROP TABLE IF EXISTS `t_right_groups`;

CREATE TABLE `t_right_groups` (
  `id` int(6) NOT NULL AUTO_INCREMENT COMMENT '组ID',
  `name` varchar(50) DEFAULT NULL COMMENT '用户组名称',
  `description` varchar(100) DEFAULT NULL COMMENT '用户组描述',
  `right_modules` varchar(100) DEFAULT NULL COMMENT '权限模块 1.2.3...10',
  PRIMARY KEY (`id`),
  KEY `index_right_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8 COMMENT='权限组信息表';

/*Table structure for table `t_right_groups_of_supervisor` */

DROP TABLE IF EXISTS `t_right_groups_of_supervisor`;

CREATE TABLE `t_right_groups_of_supervisor` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '管理员组ID',
  `supervisor_id` int(20) DEFAULT NULL COMMENT '管理员ID',
  `group_id` int(6) DEFAULT NULL COMMENT '组ID',
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  KEY `user_id` (`supervisor_id`),
  KEY `index_rgos_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8 COMMENT='管理员组权限信息表';

/*Table structure for table `t_right_types` */

DROP TABLE IF EXISTS `t_right_types`;

CREATE TABLE `t_right_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '权限类型ID',
  `name` varchar(50) DEFAULT NULL COMMENT '权限名称',
  `code` varchar(50) DEFAULT NULL COMMENT '代码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `is_use` bit(1) DEFAULT NULL COMMENT '0.可用；1.不可用',
  PRIMARY KEY (`id`),
  KEY `index_rt_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8 COMMENT='权限类型表';

/*Table structure for table `t_rights` */

DROP TABLE IF EXISTS `t_rights`;

CREATE TABLE `t_rights` (
  `id` int(6) NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `type_id` int(20) DEFAULT NULL COMMENT '权限类型ID',
  `name` varchar(50) DEFAULT NULL COMMENT '权限名称',
  `code` varchar(50) DEFAULT NULL COMMENT '代码',
  `description` varchar(200) DEFAULT NULL COMMENT '权限描述',
  PRIMARY KEY (`id`),
  KEY `index_right_id` (`id`) USING BTREE,
  KEY `index_right_type_id` (`type_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=182 DEFAULT CHARSET=utf8 COMMENT='二级栏目权限表';

/*Table structure for table `t_rights_of_group` */

DROP TABLE IF EXISTS `t_rights_of_group`;

CREATE TABLE `t_rights_of_group` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `group_id` int(20) DEFAULT NULL COMMENT '组ID',
  `right_id` int(20) DEFAULT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='权限组的权限表';

/*Table structure for table `t_rights_of_supervisor` */

DROP TABLE IF EXISTS `t_rights_of_supervisor`;

CREATE TABLE `t_rights_of_supervisor` (
  `id` int(20) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `supervisor_id` int(20) DEFAULT NULL COMMENT '管理员ID',
  `right_id` int(6) DEFAULT NULL COMMENT '权限ID',
  PRIMARY KEY (`id`),
  KEY `index_ros_id` (`id`) USING BTREE,
  KEY `index_ros_super_id` (`supervisor_id`) USING BTREE,
  KEY `index_ros_right_id` (`right_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8 COMMENT='管理员的权限表';

/*Table structure for table `t_supervisor_event_types` */

DROP TABLE IF EXISTS `t_supervisor_event_types`;

CREATE TABLE `t_supervisor_event_types` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `name` varchar(50) DEFAULT NULL COMMENT '事件名称',
  `code` varchar(50) DEFAULT NULL COMMENT '代码',
  `description` varchar(200) DEFAULT NULL COMMENT '描述',
  `is_use` bit(1) DEFAULT b'0' COMMENT '0.使用;1.不使用',
  PRIMARY KEY (`id`),
  KEY `is_use` (`is_use`),
  KEY `index_set_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8001 DEFAULT CHARSET=utf8 COMMENT='管理员事件类型信息表';

/*Table structure for table `t_supervisor_events` */

DROP TABLE IF EXISTS `t_supervisor_events`;

CREATE TABLE `t_supervisor_events` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `supervisor_id` bigint(20) DEFAULT NULL COMMENT '管理员ID',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP',
  `type_id` int(11) DEFAULT NULL COMMENT '事件类型id',
  `descrption` text COMMENT '描述',
  PRIMARY KEY (`id`),
  KEY `index_se_id` (`id`) USING BTREE,
  KEY `index_se_super_id` (`supervisor_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=48899 DEFAULT CHARSET=utf8 COMMENT='管理员事件信息表';

/*Table structure for table `t_supervisors` */

DROP TABLE IF EXISTS `t_supervisors`;

CREATE TABLE `t_supervisors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `name` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `reality_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `password` varchar(32) DEFAULT NULL COMMENT '录登密码',
  `password_continuous_errors` int(11) DEFAULT '0' COMMENT '密码连续错误次数',
  `is_password_error_locked` bit(1) DEFAULT NULL COMMENT '密码连续错误被锁定',
  `password_error_locked_time` datetime DEFAULT NULL COMMENT '密码连续错误被锁定时间',
  `is_allow_login` bit(1) DEFAULT NULL COMMENT '是否允许登录',
  `login_count` bigint(20) DEFAULT '0' COMMENT '登录次数',
  `last_login_time` datetime DEFAULT NULL COMMENT '上次登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '上次登录ip',
  `last_logout_time` datetime DEFAULT NULL COMMENT '上次退出时间',
  `last_login_city` varchar(255) DEFAULT NULL,
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `telephone` varchar(50) DEFAULT NULL COMMENT '电话号码',
  `mobile1` varchar(50) DEFAULT NULL COMMENT '手机号码1',
  `mobile2` varchar(50) DEFAULT NULL COMMENT '手机号码2',
  `office_telephone` varchar(50) DEFAULT NULL COMMENT '办公电话',
  `fax_number` varchar(50) DEFAULT NULL,
  `sex` tinyint(4) DEFAULT '3' COMMENT '1 男 2 女 3 未知',
  `birthday` datetime DEFAULT NULL COMMENT '生日',
  `level` tinyint(4) DEFAULT '0' COMMENT '0普通管理员，1超级管理员',
  `is_erased` bit(1) DEFAULT NULL COMMENT '0 = 正常状态; 1 = 已擦除状态;',
  `creater_id` bigint(20) DEFAULT NULL,
  `ukey` varchar(50) DEFAULT NULL COMMENT 'U盾密钥',
  `is_customer` bit(1) DEFAULT NULL COMMENT '是否客服 0否 1是',
  `customer_num` varchar(255) DEFAULT NULL COMMENT '客服编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `name_index` (`name`),
  KEY `creater_id` (`creater_id`),
  KEY `index_super_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='管理员账户信息表';

/*Table structure for table `t_system_options` */

DROP TABLE IF EXISTS `t_system_options`;

CREATE TABLE `t_system_options` (
  `id` smallint(6) NOT NULL AUTO_INCREMENT COMMENT '编号ID',
  `_key` varchar(50) DEFAULT NULL COMMENT '键',
  `_value` varchar(1000) DEFAULT NULL COMMENT '值',
  `description` varchar(100) DEFAULT NULL COMMENT '描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `key` (`_key`) USING BTREE,
  KEY `index_so_id` (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7007 DEFAULT CHARSET=utf8 COMMENT='系统参数设置表';

/*Table structure for table `t_transfer_batches` */

DROP TABLE IF EXISTS `t_transfer_batches`;

CREATE TABLE `t_transfer_batches` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '序号',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `batch_no` int(11) DEFAULT '0' COMMENT '分批编号',
  `bid_bill_no` varchar(100) DEFAULT '' COMMENT '发标流水号（第三方）',
  `transfer_bill_nos` blob COMMENT '转账流水号（第三方）',
  `transfer_type` int(4) DEFAULT '1' COMMENT '交易类型。1、投标  2、还款',
  `status` int(4) DEFAULT '0' COMMENT '转账是否成功。0、否  1、是',
  `memo` blob COMMENT '其他信息',
  PRIMARY KEY (`id`),
  UNIQUE KEY `index_batch_id` (`batch_no`,`bid_bill_no`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8 COMMENT='分批处理转账，缓存表';

/*Table structure for table `t_transfer_details` */

DROP TABLE IF EXISTS `t_transfer_details`;

CREATE TABLE `t_transfer_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `pmerBillNo` varchar(30) DEFAULT NULL COMMENT '流水号',
  `orderId` varchar(30) DEFAULT NULL COMMENT '订单号',
  `transAmt` decimal(20,2) DEFAULT '0.00' COMMENT '金额',
  `inCustId` varchar(30) DEFAULT NULL COMMENT '入账号-托管平台账号',
  `outCustId` varchar(30) DEFAULT NULL COMMENT '出账号-托管平台账号',
  `status` int(1) DEFAULT NULL COMMENT '交易状态0:失败、1：成功',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='转账';

/*Table structure for table `t_untreated_imformation` */

DROP TABLE IF EXISTS `t_untreated_imformation`;

CREATE TABLE `t_untreated_imformation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `count` int(11) NOT NULL,
  `gateway_id` int(11) NOT NULL,
  `information` varchar(255) DEFAULT NULL,
  `status` bit(1) NOT NULL,
  `time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='未处理成功的请求的信息';

/*View structure for view v_db_operations */

/*!50001 DROP TABLE IF EXISTS `v_db_operations` */;
/*!50001 DROP VIEW IF EXISTS `v_db_operations` */;

/*!50001 CREATE VIEW `v_db_operations` AS select `t_db_operations`.`id` AS `id`,`t_db_operations`.`time` AS `time`,`t_db_operations`.`ip` AS `ip`,`t_db_operations`.`type` AS `type`,`t_supervisors`.`name` AS `supervisor_name`,`t_supervisors`.`ukey` AS `supervisor_ukey`,`t_db_operations`.`filename` AS `filename` from (`t_db_operations` left join `t_supervisors` on((`t_db_operations`.`supervisor_id` = `t_supervisors`.`id`))) */;

/*View structure for view v_member_details */

/*!50001 DROP TABLE IF EXISTS `v_member_details` */;
/*!50001 DROP VIEW IF EXISTS `v_member_details` */;

/*!50001 CREATE VIEW `v_member_details` AS select `t_member_details`.`id` AS `id`,`t_member_details`.`platform_id` AS `platform_id`,(select `t_platforms`.`name` from `t_platforms` where (`t_platforms`.`id` = `t_member_details`.`platform_id`)) AS `platform_name`,`t_member_details`.`member_id` AS `member_id`,(select `t_member_of_platforms`.`platform_member_name` from `t_member_of_platforms` where ((`t_member_of_platforms`.`platform_id` = `t_member_details`.`platform_id`) and (`t_member_of_platforms`.`platform_member_id` = `t_member_details`.`member_id`))) AS `member_name`,`t_member_details`.`serial_number` AS `serial_number`,`t_member_details`.`time` AS `time`,`t_member_details`.`operation` AS `operation`,`t_member_details`.`amount` AS `amount`,`t_member_details`.`status` AS `status`,`t_member_detail_types`.`name` AS `name`,`t_member_details`.`summary` AS `summary` from (`t_member_details` join `t_member_detail_types` on((`t_member_details`.`operation` = `t_member_detail_types`.`id`))) */;

/*View structure for view v_member_events */

/*!50001 DROP TABLE IF EXISTS `v_member_events` */;
/*!50001 DROP VIEW IF EXISTS `v_member_events` */;

/*!50001 CREATE VIEW `v_member_events` AS select `t_member_events`.`id` AS `id`,`t_member_events`.`platform_id` AS `platform_id`,(select `t_platforms`.`name` from `t_platforms` where (`t_platforms`.`id` = `t_member_events`.`platform_id`)) AS `platform_name`,`t_member_events`.`member_id` AS `member_id`,(select `t_member_of_platforms`.`platform_member_name` from `t_member_of_platforms` where ((`t_member_of_platforms`.`platform_id` = `t_member_events`.`platform_id`) and (`t_member_of_platforms`.`platform_member_id` = `t_member_events`.`member_id`))) AS `member_name`,`t_member_events`.`time` AS `time`,`t_member_events`.`type_id` AS `type_id`,`t_member_event_types`.`name` AS `name`,`t_member_events`.`descrption` AS `descrption` from (`t_member_events` join `t_member_event_types` on((`t_member_events`.`type_id` = `t_member_event_types`.`id`))) */;

/*View structure for view v_platforms */

/*!50001 DROP TABLE IF EXISTS `v_platforms` */;
/*!50001 DROP VIEW IF EXISTS `v_platforms` */;

/*!50001 CREATE VIEW `v_platforms` AS select `t_platforms`.`id` AS `id`,`t_platforms`.`time` AS `time`,`t_platforms`.`name` AS `name`,`t_platforms`.`domain` AS `domain`,`t_platforms`.`gateway_id` AS `gateway_id`,`t_platforms`.`encryption` AS `encryption`,`t_platforms`.`status` AS `status`,`t_platforms`.`use_type` AS `use_type`,`t_platforms`.`deal_status` AS `deal_status`,`t_payment_gateways`.`name` AS `gateway` from (`t_platforms` join `t_payment_gateways` on((`t_platforms`.`gateway_id` = `t_payment_gateways`.`id`))) */;

/*View structure for view v_right_groups */

/*!50001 DROP TABLE IF EXISTS `v_right_groups` */;
/*!50001 DROP VIEW IF EXISTS `v_right_groups` */;

/*!50001 CREATE VIEW `v_right_groups` AS select `rg`.`id` AS `id`,`rg`.`name` AS `name`,`rg`.`description` AS `description`,`rg`.`right_modules` AS `right_modules`,(select count(`gos`.`id`) AS `count(gos.id)` from `t_right_groups_of_supervisor` `gos` where (`gos`.`group_id` = `rg`.`id`)) AS `supervisor_count` from `t_right_groups` `rg` */;

/*View structure for view v_supervisor_events */

/*!50001 DROP TABLE IF EXISTS `v_supervisor_events` */;
/*!50001 DROP VIEW IF EXISTS `v_supervisor_events` */;

/*!50001 CREATE VIEW `v_supervisor_events` AS select `t_supervisor_events`.`id` AS `id`,`t_supervisor_events`.`supervisor_id` AS `supervisor_id`,`t_supervisor_events`.`time` AS `time`,`t_supervisor_events`.`ip` AS `ip`,`t_supervisor_events`.`type_id` AS `type_id`,`t_supervisor_events`.`descrption` AS `descrption`,concat(`t_supervisors`.`reality_name`,`t_supervisor_events`.`descrption`) AS `content`,`t_supervisors`.`name` AS `supervisor_name`,`t_supervisors`.`level` AS `supervisor_level`,`t_supervisor_event_types`.`name` AS `type_name`,`t_supervisor_event_types`.`description` AS `type_description`,`t_supervisors`.`ukey` AS `ukey` from ((`t_supervisor_events` left join `t_supervisor_event_types` on((`t_supervisor_events`.`type_id` = `t_supervisor_event_types`.`id`))) left join `t_supervisors` on((`t_supervisor_events`.`supervisor_id` = `t_supervisors`.`id`))) */;

/*View structure for view v_supervisors */

/*!50001 DROP TABLE IF EXISTS `v_supervisors` */;
/*!50001 DROP VIEW IF EXISTS `v_supervisors` */;

/*!50001 CREATE VIEW `v_supervisors` AS select `t_supervisors`.`id` AS `id`,`t_supervisors`.`time` AS `time`,`t_supervisors`.`name` AS `name`,`t_supervisors`.`reality_name` AS `reality_name`,`t_supervisors`.`password` AS `password`,`t_supervisors`.`password_continuous_errors` AS `password_continuous_errors`,`t_supervisors`.`is_password_error_locked` AS `is_password_error_locked`,`t_supervisors`.`password_error_locked_time` AS `password_error_locked_time`,`t_supervisors`.`is_allow_login` AS `is_allow_login`,`t_supervisors`.`login_count` AS `login_count`,`t_supervisors`.`last_login_time` AS `last_login_time`,`t_supervisors`.`last_login_ip` AS `last_login_ip`,`t_supervisors`.`last_logout_time` AS `last_logout_time`,`t_supervisors`.`email` AS `email`,`t_supervisors`.`telephone` AS `telephone`,`t_supervisors`.`mobile1` AS `mobile1`,`t_supervisors`.`mobile2` AS `mobile2`,`t_supervisors`.`office_telephone` AS `office_telephone`,`t_supervisors`.`fax_number` AS `fax_number`,`t_supervisors`.`sex` AS `sex`,`t_supervisors`.`birthday` AS `birthday`,`t_supervisors`.`level` AS `level`,`t_supervisors`.`is_erased` AS `is_erased`,`t_supervisors`.`creater_id` AS `creater_id`,`t_supervisors`.`ukey` AS `ukey`,`t_supervisors`.`is_customer` AS `is_customer`,`t_supervisors`.`customer_num` AS `customer_num`,(select cast(group_concat(`rg`.`name` separator ',') as char charset utf8) AS `cast(group_concat(``name``) as char)` from (`t_right_groups` `rg` left join `t_right_groups_of_supervisor` `rgos` on((`rg`.`id` = `rgos`.`group_id`))) where (`rgos`.`supervisor_id` = `t_supervisors`.`id`)) AS `right_group` from `t_supervisors` where (isnull(`t_supervisors`.`is_erased`) or (`t_supervisors`.`is_erased` = 0)) */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
