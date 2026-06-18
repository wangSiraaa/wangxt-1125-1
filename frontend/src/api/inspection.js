import request from '@/utils/request'

export const getInspectionPage = (data) => request.post('/inspection/page', data)
export const getInspectionBySchedule = (scheduleId) => request.get(`/inspection/schedule/${scheduleId}`)
export const getInspection = (id) => request.get(`/inspection/${id}`)
export const saveInspection = (data, inspectorId) => request.post(`/inspection?inspectorId=${inspectorId}`, data)
export const updateInspection = (data) => request.put('/inspection', data)
export const recheckInspection = (id, recheckerId, recheckResult) => request.post(`/inspection/${id}/recheck?recheckerId=${recheckerId}&recheckResult=${recheckResult}`)

export const getSuspensionList = () => request.get('/inspection/suspension/list')
export const getSuspensionsBySchedule = (scheduleId) => request.get(`/inspection/suspension/schedule/${scheduleId}`)
export const resolveSuspension = (id, resolverId, resolveMeasure) => request.post(`/inspection/suspension/resolve/${id}?resolverId=${resolverId}&resolveMeasure=${encodeURIComponent(resolveMeasure)}`)
