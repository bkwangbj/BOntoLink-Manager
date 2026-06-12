export function labelFormmat (area, data, type) {
  if (type === '1') {
    return function (params) {
      const item = area.find(ele => {
        return params.name === ele.name
      })
      let form = {}
      if (item) {
        form = data.find(ele => {
          return (ele?.code?.toString() === item?.adcode?.toString()) || ele?.name === item?.name
        })
      }
      let value = form?.value || null
      if (isNaN(value)) {
        try {
          value = Number(value.replace(/,/g, ''))
        } catch (error) {

        }
      }
      return value ? `${params.name}:(${value})` : ''
    }
  } else if (type === '2') {
    return function (params) {
      return `${params.name}`
    }
  } else if (type === '3') {
    return function (params) {
      const item = area.find(ele => {
        return params.name === ele.name
      })
      let form = {}
      if (item) {
        form = data.find(ele => {
          return (ele?.code?.toString() === item?.adcode?.toString()) || ele?.name === item?.name
        })
      }
      let value = form?.value || null
      if (isNaN(value)) {
        try {
          value = Number(value.replace(/,/g, ''))
        } catch (error) {

        }
      }
      return value || ''
    }
  }
}
