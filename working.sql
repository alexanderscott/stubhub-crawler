select * from listings

select * from listing_updates

delete from listing_updates;
delete from listings;

select lu.listing_id, lu.update_time, lu.price, lu.quantity, lu.seats
from listing_updates lu
join listings l on l.id = lu.listing_id
where l.event_id = 9177358
and lu.quantity != 0
order by lu.update_time

select * from listing_updates
where listing_id in (1138974774, 1141387467, 1145007736, 1142205080)
order by listing_id, update_time

select lu.listing_id, lu.update_time, lu.price, lu.quantity, lu.seats
from listing_updates lu
join listings l on l.id = lu.listing_id
where l.event_id = 9177358
order by lu.listing_id, lu.update_time

select * from listing_updates lu where lu.listing_id in
	(select listing_id from listing_updates where update_time = '2015-04-12 23:29:50.486')
order by lu.listing_id, lu.update_time

 from listing_updates where update_time = '2015-04-12 22:34:18.489'
select count(*) from listing_updates