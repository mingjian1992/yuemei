
-- ----------------------------
-- Table structure for `t_yee_req_params`
-- ----------------------------
DROP TABLE IF EXISTS `t_yee_req_params`;
CREATE TABLE `t_yee_req_params` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `memberId` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `oprateType` int(3) DEFAULT NULL COMMENT '操作类型ID',
  `oprateTypeName` varchar(40) DEFAULT NULL COMMENT '操作类型名称',
  `requestNo` varchar(50) DEFAULT NULL COMMENT '请求流水号',
  `reqValue` text COMMENT 'req值',
  `sign` text COMMENT '加签参数',
  `url` varchar(200) DEFAULT NULL COMMENT '地址',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=427 DEFAULT CHARSET=utf8;


-- ----------------------------
-- Table structure for `t_yee_resq_params`
-- ----------------------------
DROP TABLE IF EXISTS `t_yee_resq_params`;
CREATE TABLE `t_yee_resq_params` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `requestNo` varchar(40) DEFAULT NULL COMMENT '请求流水号',
  `code` int(3) DEFAULT NULL COMMENT '状态码',
  `ayns` bigint(1) DEFAULT NULL COMMENT '是否异步',
  `result` text COMMENT '结果',
  `sign` text COMMENT '加签',
  `url` text COMMENT '地址',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=219 DEFAULT CHARSET=utf8;
