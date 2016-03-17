with open_shop_orders as (
  select
    so.contract,
    so.order_no,
    so.release_no,
    so.sequence_no,
    so.part_no,
    so.priority_category,
    ifsapp.lpe_shop_ord_util_api.earliest_op_with_os_qty(
      so.order_no,
      so.release_no,
      so.sequence_no
    ) first_open_op
  from ifsapp.shop_ord so
  where so.close_date is null
    and so.objstate not in ('Planned', 'Closed', 'Cancelled')
    and (
      so.priority_category is null or
      so.priority_category not like '^%'
    )
),
clocked_ops as (
  select distinct
    oc.op_id
  from ifsapp.op_clocking_tab oc
  where trunc(oc.account_date) = trunc(sysdate)
    and oc.start_stamp is not null
    and oc.stop_stamp is null
),
sequenced_ops as (
  select
    soo.order_no,
    soo.release_no,
    soo.sequence_no,
    soo.operation_no,
    soo.qty_complete,
    rank () over (
      partition by
        soo.order_no,
        soo.release_no,
        soo.sequence_no
      order by
        soo.operation_no
    ) operation_sequence
  from open_shop_orders so
  join ifsapp.shop_order_operation soo
    on so.order_no = soo.order_no
    and so.release_no = soo.release_no
    and so.sequence_no = soo.sequence_no
),
first_ops as (
  select
    (
      so.order_no || '-' ||
      so.release_no || '-' ||
      so.sequence_no
    ) as order_id,
    so.order_no,
    so.release_no as order_release,
    so.sequence_no as order_sequence,
    soo.operation_no as op_no,
    soo.op_id,
    wc.work_center_no as work_center_id,
    wc.description as work_center_description,
    ip.part_no as part_id,
    ipcp.cust_part_no as part_drawing_number,
    ipcp.issue as part_issue,
    ipcp.description as part_description,
    nvl(bs.info, 'BLUE') as buffer_zone,
    nvl(bs.value_no, -100) as buffer_penetration,
    soo.revised_qty_due as qty,
    greatest(
      soo.revised_qty_due - soo.qty_complete - soo.qty_scrapped,
      0
    ) as qty_avail,
    --
    ip.planner_buyer as planner_id,
    wc.production_line as production_line_id,
    substr(wc.department_no, 1, instr(wc.department_no, '-')-1) as manager_id,
    om.mch_type as terminal_id
  from open_shop_orders so
  --
  join sequenced_ops ops
    on so.order_no = ops.order_no
    and so.release_no = ops.release_no
    and so.sequence_no = ops.sequence_no
    and ops.operation_sequence = 1
    and ops.operation_no = so.first_open_op
  --
  join ifsapp.shop_order_operation soo
    on so.order_no = soo.order_no
    and so.release_no = soo.release_no
    and so.sequence_no = soo.sequence_no
    and soo.operation_no = ops.operation_no
  --
  join ifsapp.work_center wc
    on soo.contract = wc.contract
    and wc.work_center_no =
      case
        when soo.outside_op_item is not null
         and ifsapp.lpe_shop_ord_util_api.get_outside_qty_to_inspect(
            so.order_no, so.release_no, so.sequence_no, soo.operation_no
          ) > 0
        then
          'IN002'
        when not exists (
            select
              *
            from sequenced_ops ops2
            where so.order_no = ops2.order_no
              and so.release_no = ops2.release_no
              and so.sequence_no = ops2.sequence_no
              and ops2.operation_sequence > ops.operation_sequence
          )
         and soo.revised_qty_due <= (
            soo.qty_complete + soo.qty_scrapped
          )
        then
          'PR017'
        else
          soo.work_center_no
      end
  join ifsapp.op_machine om
    on wc.contract = om.contract
    and wc.work_center_no = om.work_center_no
    and om.mch_code = ifsapp.work_center_resource_api.get_active_resource(
      wc.contract, wc.work_center_no
    )
  --
  join ifsapp.technical_object_reference tor
    on tor.lu_name = 'ShopOrd'
    and tor.key_value = (
      so.order_no || '^' ||
      so.release_no || '^' ||
      so.sequence_no || '^'
    )
  join ifsapp.technical_spec_numeric bs
    on tor.technical_spec_no = bs.technical_spec_no
    and bs.attribute = 'DBR_BUFFER_PEN'
  --
  join ifsapp.inventory_part ip
    on so.contract = ip.contract
    and so.part_no = ip.part_no
  join ifsinfo.inv_part_cust_part_no ipcp
    on so.part_no = ipcp.part_no
  --
  left outer join clocked_ops co
    on soo.op_id = co.op_id
  --
  where co.op_id is not null
     or soo.revised_qty_due > (soo.qty_complete + soo.qty_scrapped)
),
rest_ops as (
  select
    (
      so.order_no || '-' ||
      so.release_no || '-' ||
      so.sequence_no
    ) as order_id,
    so.order_no,
    so.release_no as order_release,
    so.sequence_no as order_sequence,
    soo.operation_no as op_no,
    soo.op_id,
    wc.work_center_no as work_center_id,
    wc.description as work_center_description,
    ip.part_no as part_id,
    ipcp.cust_part_no as part_drawing_number,
    ipcp.issue as part_issue,
    ipcp.description as part_description,
    nvl(bs.info, 'BLUE') as buffer_zone,
    nvl(bs.value_no, -100) as buffer_penetration,
    soo.revised_qty_due as qty,
    greatest(
      psoo.qty_complete - soo.qty_complete - soo.qty_scrapped,
      0
    ) as qty_avail,
    --
    ip.planner_buyer as planner_id,
    wc.production_line as production_line_id,
    substr(wc.department_no, 1, instr(wc.department_no, '-')-1) as manager_id,
    om.mch_type as terminal_id
  from open_shop_orders so
  --
  join ifsapp.shop_order_operation soo
    on so.order_no = soo.order_no
    and so.release_no = soo.release_no
    and so.sequence_no = soo.sequence_no
  --
  join sequenced_ops ops
    on so.order_no = ops.order_no
    and so.release_no = ops.release_no
    and so.sequence_no = ops.sequence_no
    and soo.operation_no = ops.operation_no
  join sequenced_ops psoo
    on so.order_no = psoo.order_no
    and so.release_no = psoo.release_no
    and so.sequence_no = psoo.sequence_no
    and psoo.operation_sequence = (ops.operation_sequence - 1)
  --
  join ifsapp.work_center wc
    on soo.contract = wc.contract
    and wc.work_center_no =
      case
        when soo.outside_op_item is not null
         and ifsapp.lpe_shop_ord_util_api.get_outside_qty_to_inspect(
            so.order_no, so.release_no, so.sequence_no, soo.operation_no
          ) > 0
        then
          'IN002'
        when not exists (
            select
              *
            from sequenced_ops ops2
            where so.order_no = ops2.order_no
              and so.release_no = ops2.release_no
              and so.sequence_no = ops2.sequence_no
              and ops2.operation_sequence > ops.operation_sequence
          )
         and soo.revised_qty_due <= (
            soo.qty_complete + soo.qty_scrapped
          )
        then
          'PR017'
        else
          soo.work_center_no
      end
  join ifsapp.op_machine om
    on wc.contract = om.contract
    and wc.work_center_no = om.work_center_no
    and om.mch_code = ifsapp.work_center_resource_api.get_active_resource(
      wc.contract, wc.work_center_no
    )
  --
  join ifsapp.technical_object_reference tor
    on tor.lu_name = 'ShopOrd'
    and tor.key_value = (
      so.order_no || '^' ||
      so.release_no || '^' ||
      so.sequence_no || '^'
    )
  join ifsapp.technical_spec_numeric bs
    on tor.technical_spec_no = bs.technical_spec_no
    and bs.attribute = 'DBR_BUFFER_PEN'
  --
  join ifsapp.inventory_part ip
    on so.contract = ip.contract
    and so.part_no = ip.part_no
  join ifsinfo.inv_part_cust_part_no ipcp
    on so.part_no = ipcp.part_no
  --
  left outer join clocked_ops co
    on soo.op_id = co.op_id
  --
  where co.op_id is not null
    or (
      soo.operation_no >= so.first_open_op
      and psoo.qty_complete > (soo.qty_complete + soo.qty_scrapped)
      and soo.revised_qty_due > (soo.qty_complete + soo.qty_scrapped)
    )
)
select * from first_ops
union
select * from rest_ops
