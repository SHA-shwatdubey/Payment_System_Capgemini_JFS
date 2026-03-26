FROM grafana/grafana:latest

ENV GF_SECURITY_ADMIN_USER=admin
ENV GF_SECURITY_ADMIN_PASSWORD=admin123
ENV GF_SECURITY_DISABLE_BRUTE_FORCE_LOGIN_PROTECTION=true
ENV GF_INSTALL_PLUGINS=grafana-piechart-panel

COPY prometheus-datasource.yml /etc/grafana/provisioning/datasources/prometheus.yml
COPY dashboard-provider.yml /etc/grafana/provisioning/dashboards/provider.yml

EXPOSE 3000

ENTRYPOINT ["/run.sh"]

