cp ~/Scorebert/scores.db ~/Scorebert/backups/scores-$(date +%s).db
sqlite3 ~/Scorebert/scores.db ".tables" | awk '{for(i=1;i<=NF;i++) print "UPDATE "$i " SET REMAINING_POINTS=\04710\047;"}' | sqlite3 ~/Scorebert/scores.db
