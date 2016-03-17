select
  oc.emp_no as employee_id,
  ep.fname as employee_given_name,
  ep.lname as employee_family_name,
  --
  sup.employee_id as supervisor_id,
  sp.fname as supervisor_given_name,
  sp.lname as supervisor_family_name,
  --
  oc.op_id,
  oc.start_stamp as started_at
from ifsapp.op_clocking_tab oc
--
join ifsapp.company_emp ce
  on oc.company_id = ce.company
  and oc.emp_no = ce.employee_id
join ifsapp.pers ep
  on ce.person_id = ep.person_id
--
join ifsapp.company_emp sup
  on oc.company_id = sup.company
  and sup.employee_id =
    ifsapp.company_pers_assign_api.get_primary_sup_emp_no(
      oc.company_id,
      oc.emp_no,
      sysdate
    )
join ifsapp.pers sp
  on sup.person_id = sp.person_id
--
where trunc(oc.account_date) = trunc(sysdate)
  and oc.start_stamp is not null
  and oc.stop_stamp is null
