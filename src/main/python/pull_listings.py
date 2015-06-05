import psycopg2

try:
    conn = psycopg2.connect(pg_config.conn_string)
except:
    print "can't connect to db"

c = conn.cursor()
c.execute("""SELECT * FROM listing_updates
             WHERE LISTING_ID IN (1140180646)
             ORDER BY listing_id, update_time DESC""")

rows = c.fetchall()

id_to_listings = {}
