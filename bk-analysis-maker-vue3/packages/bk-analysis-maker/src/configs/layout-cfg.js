export default [
  {
    id: 'a',
    title: '左右布局',
    configs: {
      direction: 'row',
      gap: 10,
      id: '1',
      children: [
        {
          id: '1-1',
          direction: 'column',
          gap: 10,
          width: '55%',
          children: [
            { id: '1-1-1', height: '500px' },
            { id: '1-1-2', height: '500px' },
            { id: '1-1-3', height: '500px' }
          ]
        },
        { id: '2-1', width: '45%' }
      ]
    }
  },
  {
    id: 'b',
    title: '上下布局',
    configs: {
      direction: 'column',
      gap: 10,
      id: '1',
      children: [
        {
          id: '1-1',
          height: '60%',
          direction: 'row',
          gap: 5,
          children: [
            {
              id: '1-1-1',
              width: '30%'
            },
            { id: '1-1-2', width: '50%' },
            { id: '1-1-3', width: '20%' }
          ]
        },
        {
          id: '1-2',
          height: '40%',
          direction: 'row',
          gap: 10,
          children: [
            {
              id: '1-2-1',
              width: '50%'
            },
            { id: '1-2-2', width: '50%' }
          ]
        }

      ]
    }
  }
]
