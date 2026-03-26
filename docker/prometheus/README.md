# Prometheus Configuration

This directory contains Prometheus server configuration for monitoring all microservices.

## Files

- **Dockerfile** - Prometheus container image
- **prometheus.yml** - Main Prometheus configuration (targets, scrape interval, alert rules)
- **alert-rules.yml** - Alert rules and conditions

## Services Monitored

Prometheus scrapes metrics from 11 microservices:

1. API Gateway (8062)
2. Auth Service (8063)
3. User KYC Service (8064)
4. Wallet Service (8065)
5. Rewards Service (8066)
6. Admin Service (8067)
7. Transaction Service (8068)
8. Notification Service (8069)
9. Integration Service (8070)
10. Eureka Server (8061)
11. Config Server (8060)

## Metrics Endpoint

All services expose metrics at: `http://<service>:<port>/actuator/prometheus`

## Build & Run

```bash
# From project root
docker-compose build prometheus
docker-compose up -d prometheus
```

## Access Prometheus UI

http://localhost:9090

## Key Features

- **Scrape Interval**: 15 seconds
- **Data Retention**: 30 days
- **Alert Evaluation**: Every 15 seconds
- **Alert Rules**: 6 configured alerts
- **Storage**: prometheus_data volume

## Configuration

Edit `prometheus.yml` to:
- Add/remove scrape targets
- Change scrape interval
- Modify retention policy

Edit `alert-rules.yml` to:
- Add new alerts
- Modify alert thresholds
- Change alert durations

## Debugging

```bash
# View Prometheus logs
docker-compose logs prometheus

# Check targets
curl http://localhost:9090/api/v1/targets

# Test query
curl http://localhost:9090/api/v1/query?query=up

# Check alerts
curl http://localhost:9090/api/v1/alerts
```

## Notes

- Prometheus uses a TSDB for time-series data storage
- All targets must be reachable from Prometheus container
- Metrics are scraped via HTTP, services must expose /actuator/prometheus
- Alert rules are evaluated at regular intervals

---

**Status**: ✅ Ready
**Last Updated**: March 26, 2026

