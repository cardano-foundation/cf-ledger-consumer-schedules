drop materialized view tx_chart;

CREATE MATERIALIZED VIEW IF NOT EXISTS tx_chart as SELECT EXTRACT(epoch FROM date_trunc('minute', simple_tx_tmp.tx_time))  AS minute,
                                            EXTRACT(epoch FROM date_trunc('hour', simple_tx_tmp.tx_time))    AS hour,
                                            EXTRACT(epoch FROM date_trunc('day', simple_tx_tmp.tx_time))     AS day,
                                            EXTRACT(epoch FROM date_trunc('month', simple_tx_tmp.tx_time))   AS month,
                                            EXTRACT(epoch FROM date_trunc('year', simple_tx_tmp.tx_time))    AS year,
                                            COUNT(simple_tx_tmp.tx_id)                                       AS tx_count,
                                            SUM(CASE WHEN simple_tx_tmp.tx_with_sc = true THEN 1 ELSE 0 END) AS tx_with_sc,
                                            SUM(CASE
                                                    WHEN simple_tx_tmp.tx_with_metadata_without_sc = true THEN 1
                                                    ELSE 0 END)                                              AS tx_with_metadata_without_sc,
                                            SUM(CASE WHEN simple_tx_tmp.simple_tx = true THEN 1 ELSE 0 END)  AS tx_simple
                                                   FROM (SELECT tx.id                                                                  AS tx_id,
                                                       b.time                                                                 AS tx_time,
                                                       (SUM(CASE WHEN r.id IS NOT NULL THEN 1 ELSE 0 END) != 0)               AS tx_with_sc,
                                                       (SUM(CASE WHEN r.id IS NULL AND tm.id IS NOT NULL THEN 1 ELSE 0 END) != 0)
                                                       AS tx_with_metadata_without_sc,
                                                       (SUM(CASE WHEN r.id IS NULL AND tm.id IS NULL THEN 1 ELSE 0 END) != 0) AS simple_tx
                                                       FROM tx tx
                                                       JOIN block b ON tx.block_id = b.id
                                                       LEFT JOIN redeemer r ON tx.id = r.tx_id
                                                       LEFT JOIN tx_metadata tm ON tx.id = tm.tx_id
                                                       GROUP BY tx.id, b.time) simple_tx_tmp
                                                   GROUP BY minute, hour, day, month, year
                                                   ORDER BY minute;

CREATE UNIQUE INDEX IF NOT EXISTS unique_tx_chart_idx ON tx_chart (minute,hour,day,month,year);
CREATE INDEX IF NOT EXISTS idx_mat_tx_chart_minute ON tx_chart ("minute");
CREATE INDEX IF NOT EXISTS idx_mat_tx_chart_hour ON tx_chart ("hour");
CREATE INDEX IF NOT EXISTS idx_mat_tx_chart_day ON tx_chart ("day");
CREATE INDEX IF NOT EXISTS idx_mat_tx_chart_month ON tx_chart ("month");
