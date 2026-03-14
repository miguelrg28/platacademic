import {
    BarElement,
    CategoryScale,
    Chart as ChartJS,
    Legend,
    LinearScale,
    LineElement,
    PointElement,
    Tooltip,
} from 'chart.js'
import { Bar, Line } from 'react-chartjs-2'

import { formatDate, formatHour, percentage } from '../lib/formatters'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Tooltip, Legend)

export function SummaryCharts({ summary }) {
    if (!summary) {
        return (
            <div className="empty-state empty-state--compact">
                <h3>Selecciona un evento</h3>
                <p>
                    Al escoger un evento gestionado verás inscripciones por día, asistencia por hora
                    y el resumen general.
                </p>
            </div>
        )
    }

    const registrationLabels = summary.registrationsByDay.map((metric) => formatDate(metric.day))
    const registrationData = summary.registrationsByDay.map((metric) => metric.count)

    const attendanceLabels = summary.attendanceByHour.map((metric) => formatHour(metric.hour))
    const attendanceData = summary.attendanceByHour.map((metric) => metric.count)

    return (
        <div className="summary-grid">
            <div className="metric-band">
                <article className="metric-card">
                    <span>Inscritos activos</span>
                    <strong>{summary.totalRegistered}</strong>
                </article>
                <article className="metric-card">
                    <span>Asistentes</span>
                    <strong>{summary.totalAttendees}</strong>
                </article>
                <article className="metric-card">
                    <span>Tasa de asistencia</span>
                    <strong>{percentage(summary.attendancePercentage)}</strong>
                </article>
            </div>

            <article className="chart-card">
                <h3>Inscripciones por día</h3>
                <div className="chart-canvas-wrapper">
                    <Line
                        data={{
                            labels: registrationLabels,
                            datasets: [
                                {
                                    label: 'Inscripciones',
                                    data: registrationData,
                                    borderColor: '#d75b32',
                                    backgroundColor: 'rgba(215, 91, 50, 0.18)',
                                    tension: 0.28,
                                    fill: true,
                                },
                            ],
                        }}
                        options={{
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    display: false,
                                },
                            },
                        }}
                    />
                </div>
            </article>

            <article className="chart-card">
                <h3>Asistencia por hora</h3>
                <div className="chart-canvas-wrapper">
                    <Bar
                        data={{
                            labels: attendanceLabels,
                            datasets: [
                                {
                                    label: 'Asistencias',
                                    data: attendanceData,
                                    backgroundColor: '#1f6f69',
                                    borderRadius: 12,
                                },
                            ],
                        }}
                        options={{
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    display: false,
                                },
                            },
                        }}
                    />
                </div>
            </article>
        </div>
    )
}
