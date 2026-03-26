# Grafana Configuration

This directory contains Grafana dashboard server configuration for visualizing Prometheus metrics.

## Files

- **Dockerfile** - Grafana container image
- **prometheus-datasource.yml** - Prometheus data source configuration
- **dashboard-provider.yml** - Dashboard provisioning configuration

## Features

- **Datasource**: Pre-configured Prometheus connection
- **Default Credentials**: admin / admin123
- **Port**: 3000
- **Default Dashboard Provider**: `/etc/grafana/provisioning/dashboards`

## Build & Run

```bash
# From project root
docker-compose build grafana
docker-compose up -d grafana
```

## Access Grafana

http://localhost:3000

## Login

- **Username**: admin
- **Password**: admin123

## First Steps

1. Login to Grafana
2. Navigate to "Dashboards" in left sidebar
3. Create new dashboard
4. Add panels with Prometheus queries
5. Set time range and refresh intervals

## Add Custom Dashboard

1. Click **+** (Create) button
2. Select **Dashboard**
3. Click **Add Panel**
4. Select **Prometheus** as data source
5. Write PromQL query
6. Customize panel settings
7. Save dashboard

## Sample Queries

### Service Availability
```promql
up{job="wallet-service"}
```

### Request Rate
```promql
rate(http_server_requests_seconds_count[5m])
```

### Error Rate
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

### Response Time p95
```promql
histogram_quantile(0.95, http_server_requests_seconds_bucket)
```

### Memory Usage
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

## Configuration

### Datasource
Edit `prometheus-datasource.yml`:
- Change Prometheus URL
- Set default datasource
- Configure authentication

### Provisioning
Edit `dashboard-provider.yml`:
- Add dashboard folders
- Configure dashboard update intervals
- Set permissions

## Environment Variables

In `Dockerfile`:
- `GF_SECURITY_ADMIN_USER` - Admin username (default: admin)
- `GF_SECURITY_ADMIN_PASSWORD` - Admin password (default: admin123)
- `GF_INSTALL_PLUGINS` - Additional plugins to install

## Security

**Development Mode**:
- No authentication required
- Default credentials enabled

**Production Mode**:
1. Change default password immediately
2. Enable HTTPS/TLS
3. Configure OAuth/LDAP authentication
4. Use reverse proxy
5. Enable audit logging

## Debugging

```bash
# View Grafana logs
docker-compose logs grafana

# Check if running
curl http://localhost:3000

# Test Prometheus connection
# In Grafana UI: Connections > Datasources > Prometheus
```

## Volumes

- `grafana_data` - Persistent storage for dashboards and user data

## Notes

- Grafana automatically discovers Prometheus datasource
- Dashboards are stored in database (grafana_data volume)
- Pre-configured dashboards can be imported as JSON
- Plugins can be installed via environment variables

## Documentation

- Grafana Docs: https://grafana.com/docs/grafana/latest/
- PromQL Queries: https://prometheus.io/docs/prometheus/latest/querying/basics/
- Dashboard Building: https://grafana.com/docs/grafana/latest/dashboards/

---

**Status**: ✅ Ready
**Last Updated**: March 26, 2026

