select
  op.op_id id,
  op.optional1 code,
  op.op_desc description,
  wcr.description category
from ifsapp.op_plan op
join ifsapp.work_center_resource wcr
  on op.contract = wcr.contract
  and op.work_center_no = wcr.work_center_no
  and op.plan_mch_code = wcr.resource_id
where op.order_no = 'INDR'
  and op.op_type_db = 'IN'
  and op.op_status_db <> '90'
