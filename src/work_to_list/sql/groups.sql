select distinct
  'terminal' as type,
  om.mch_type as id,
  om.mch_type as name
from ifsapp.op_machine om
where om.mch_type is not null
  and regexp_like(om.mch_type, '^\d+$')
--
union
--
select distinct
  'manager' as type,
  substr(department_no, 1, instr(department_no, '-')-1) as id,
  trim(substr(description, 1, instr(description, '/')-1)) as name
from ifsapp.work_center_department
--
union
--
select
  'production-line' as type,
  production_line as id,
  description as name
from ifsapp.production_line pl
