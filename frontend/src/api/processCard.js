import request from '@/utils/request'

export const getProcessCardPage = (data) => request.post('/processCard/page', data)
export const getProcessCardList = () => request.get('/processCard/list')
export const getIssuedProcessCardList = () => request.get('/processCard/issued')
export const getProcessCard = (id) => request.get(`/processCard/${id}`)
export const saveProcessCard = (data) => request.post('/processCard', data)
export const updateProcessCard = (data) => request.put('/processCard', data)
export const deleteProcessCard = (id) => request.delete(`/processCard/${id}`)
export const approveProcessCard = (id) => request.post(`/processCard/${id}/approve`)
export const issueProcessCard = (id, issuerId) => request.post(`/processCard/${id}/issue?issuerId=${issuerId}`)
