package org.cardanofoundation.job.repository.ledgersyncagg.jooq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
public class JOOQAddressTxCountRepository {

    private final JdbcTemplate jdbcTemplate;

    public JOOQAddressTxCountRepository(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    /**
     * Inserts or updates address transaction counts in the database for a given range.
     *
     * @param from      Starting ID of the range (inclusive).
     * @param to        Ending ID of the range (inclusive).
     * @param batchSize The size of each batch to be processed in one go.
     */
    @Transactional
    public void insertAddressTxCount(long from, long to, long batchSize) {
        long currentFrom = from;


        // Loop through the range, processing in batches
        while (currentFrom <= to) {

            // Calculate the end value for the current batch
            long currentTo = Math.min(currentFrom + batchSize - 1, to);


            // SQL query for batch insert/update
            String sql = String.format("""
                    INSERT INTO preprod_aggregation.address_tx_count (address, tx_count)
                    SELECT ata.address,
                           COUNT(DISTINCT ata.tx_hash) AS tx_count
                    FROM preprod_aggregation.address_tx_amount ata
                    INNER JOIN preprod_aggregation.address a ON a.address = ata.address
                    WHERE a.id BETWEEN %d AND %d
                    GROUP BY ata.address
                    ON CONFLICT (address)
                    DO UPDATE SET tx_count = EXCLUDED.tx_count;
                    """, currentFrom, currentTo);

            // Execute the batch insert/update query with the current range
            log.info("insertAddressTxCount with query {}", sql);
            jdbcTemplate.execute(sql);

            // Move to the next batch
            currentFrom = currentTo + 1;
        }
    }
}
